package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositeEntityCrudEventsTest {

    private final List<String> calls = new ArrayList<>();

    private EntityCrudEvents<String, Long, String> named(String name) {
        return new EntityCrudEvents<>() {
            @Override
            public void onFind(String entity) {
                calls.add(name + ":find:" + entity);
            }

            @Override
            public void onBeforeCreate(String input, String entity) {
                calls.add(name + ":beforeCreate:" + input + "/" + entity);
            }

            @Override
            public void eachEntity(String entity) {
                calls.add(name + ":each:" + entity);
            }
        };
    }

    @Test
    @DisplayName("sin delegados devuelve el manejador por defecto")
    void noDelegatesYieldsTheDefault() {
        assertSame(EntityCrudEvents.getDefault(), EntityCrudEvents.<String, Long, String>of());
    }

    @Test
    @DisplayName("con un solo delegado lo devuelve tal cual, sin envolverlo")
    void singleDelegateIsReturnedAsIs() {
        var only = named("a");

        assertSame(only, EntityCrudEvents.of(only));
    }

    @Test
    @DisplayName("el manejador por defecto se descarta al componer")
    void defaultDelegateIsSkipped() {
        var only = named("a");

        assertSame(only, EntityCrudEvents.of(EntityCrudEvents.getDefault(), only, EntityCrudEvents.getDefault()));
    }

    @Test
    @DisplayName("despacha cada evento a los delegados en el orden dado")
    void dispatchesInOrder() {
        var events = EntityCrudEvents.of(named("a"), named("b"));

        events.onFind("x");
        events.onBeforeCreate("in", "x");
        events.eachEntity("x");

        assertIterableEquals(
            List.of("a:find:x", "b:find:x", "a:beforeCreate:in/x", "b:beforeCreate:in/x", "a:each:x", "b:each:x"),
            calls
        );
    }

    @Test
    @DisplayName("componer una composición aplana los delegados en vez de anidarla")
    void nestedCompositionIsFlattened() {
        var inner = EntityCrudEvents.of(named("a"), named("b"));
        var outer = EntityCrudEvents.of(inner, named("c"));

        outer.onFind("x");

        assertIterableEquals(List.of("a:find:x", "b:find:x", "c:find:x"), calls);
    }

    @Test
    @DisplayName("andAll suma otros manejadores a continuación del actual")
    void andAllAppends() {
        var events = named("a").andAll(List.of(named("b"), named("c")));

        events.onFind("x");

        assertIterableEquals(List.of("a:find:x", "b:find:x", "c:find:x"), calls);
    }

    @Test
    @DisplayName("se detiene en el primer delegado que lanza, sin llamar a los siguientes")
    void stopsAtTheFirstThatThrows() {
        var failing = new EntityCrudEvents<String, Long, String>() {
            @Override
            public void onFind(String entity) {
                calls.add("failing");
                throw new IllegalStateException("boom");
            }
        };
        var events = EntityCrudEvents.of(named("a"), failing, named("c"));

        assertThrows(IllegalStateException.class, () -> events.onFind("x"));

        assertIterableEquals(List.of("a:find:x", "failing"), calls);
    }

    @Test
    @DisplayName("un delegado nulo se rechaza")
    void nullDelegateIsRejected() {
        var delegates = new ArrayList<EntityCrudEvents<String, Long, String>>();
        delegates.add(named("a"));
        delegates.add(null);

        assertThrows(NullPointerException.class, () -> EntityCrudEvents.of(delegates));
    }

    @Test
    @DisplayName("los eventos de conteo, existencia y página también llegan a todos los delegados")
    void otherEventsReachEveryDelegate() {
        var counts = new ArrayList<Long>();
        var exists = new ArrayList<Boolean>();
        var pages = new ArrayList<Page<String>>();
        var recording = new EntityCrudEvents<String, Long, String>() {
            @Override
            public void onCount(long count) {
                counts.add(count);
            }

            @Override
            public void onExists(boolean found, Long id) {
                exists.add(found);
            }

            @Override
            public void onPage(Page<String> page) {
                pages.add(page);
            }
        };
        var events = EntityCrudEvents.of(recording, recording);
        var page = new Page<>(List.of("x"));

        events.onCount(3);
        events.onExists(true, 1L);
        events.onPage(page);

        assertEquals(List.of(3L, 3L), counts);
        assertEquals(List.of(true, true), exists);
        assertEquals(List.of(page, page), pages);
    }
}
