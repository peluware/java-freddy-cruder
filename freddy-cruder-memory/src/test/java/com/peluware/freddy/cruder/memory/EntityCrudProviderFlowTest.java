package com.peluware.freddy.cruder.memory;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.CrudOperation;
import com.peluware.freddy.cruder.EntityCrudEvents;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica el flujo que {@code EntityCrudProvider} impone a cada operación: en qué orden llegan los
 * ganchos y los eventos, y qué se omite cuando algo falla.
 */
class EntityCrudProviderFlowTest {

    private final List<String> calls = new ArrayList<>();

    private final EntityCrudEvents<Person, Long, PersonInput> events = new EntityCrudEvents<>() {
        @Override
        public void onFind(Person entity) {
            calls.add("onFind");
        }

        @Override
        public void onCount(long count) {
            calls.add("onCount:" + count);
        }

        @Override
        public void onExists(boolean exists, Long id) {
            calls.add("onExists:" + exists + ":" + id);
        }

        @Override
        public void onPage(Page<Person> page) {
            calls.add("onPage");
        }

        @Override
        public void onBeforeCreate(PersonInput input, Person entity) {
            calls.add("onBeforeCreate:" + entity.getName());
        }

        @Override
        public void onAfterCreate(PersonInput input, Person entity) {
            calls.add("onAfterCreate:" + entity.getId());
        }

        @Override
        public void onBeforeUpdate(PersonInput input, Person entity) {
            calls.add("onBeforeUpdate:" + entity.getName());
        }

        @Override
        public void onAfterUpdate(PersonInput input, Person entity) {
            calls.add("onAfterUpdate");
        }

        @Override
        public void onBeforeDelete(Person entity) {
            calls.add("onBeforeDelete");
        }

        @Override
        public void onAfterDelete(Person entity) {
            calls.add("onAfterDelete");
        }

        @Override
        public void eachEntity(Person entity) {
            calls.add("each:" + entity.getName());
        }
    };

    private final RecordingProvider people = new RecordingProvider(events);

    private class RecordingProvider extends MemoryCrudProvider<Person, Long, PersonInput, PersonOutput> {

        RecordingProvider(EntityCrudEvents<Person, Long, PersonInput> events) {
            super(MemoryIds.sequentialLong(Person::getId, Person::setId), Person.class, events);
        }

        @Override
        protected void mapInput(PersonInput input, Person entity, boolean isNew) {
            calls.add("mapInput:" + isNew);
            entity.name = input.name();
            entity.age = input.age();
        }

        @Override
        protected PersonOutput mapOutput(Person entity) {
            return new PersonOutput(entity.id, entity.name, entity.age);
        }

        @Override
        protected void preProcess(CrudOperation operation) {
            calls.add("pre:" + operation);
        }

        @Override
        protected void postProcess(CrudOperation operation) {
            calls.add("post:" + operation);
        }

        @Override
        protected void afterCreate(PersonInput input, Person created) {
            calls.add("afterCreate");
        }

        @Override
        protected void afterUpdate(PersonInput input, Person updated) {
            calls.add("afterUpdate");
        }

    }

    @Test
    @DisplayName("create recorre los ganchos y eventos en su orden")
    void createFlow() {
        people.create(new PersonInput("Ana", 30));

        assertIterableEquals(
            List.of("pre:CREATE", "mapInput:true", "onBeforeCreate:Ana", "afterCreate",
                "onAfterCreate:1", "each:Ana", "post:CREATE"),
            calls
        );
    }

    @Test
    @DisplayName("update carga, mapea con isNew falso y recorre los ganchos en su orden")
    void updateFlow() {
        people.create(new PersonInput("Ana", 30));
        calls.clear();

        people.update(1L, new PersonInput("Ana María", 31));

        assertIterableEquals(
            List.of("pre:UPDATE", "mapInput:false", "onBeforeUpdate:Ana María", "afterUpdate",
                "onAfterUpdate", "each:Ana María", "post:UPDATE"),
            calls
        );
    }

    @Test
    @DisplayName("delete avisa antes y después de eliminar")
    void deleteFlow() {
        people.create(new PersonInput("Ana", 30));
        calls.clear();

        people.delete(1L);

        assertIterableEquals(List.of("pre:DELETE", "onBeforeDelete", "onAfterDelete", "post:DELETE"), calls);
    }

    @Test
    @DisplayName("find, exists y count avisan a los eventos con su resultado")
    void readFlows() {
        people.create(new PersonInput("Ana", 30));
        calls.clear();

        people.find(1L);
        people.exists(1L);
        people.exists(9L);
        people.count(null, null);

        assertIterableEquals(
            List.of("pre:FIND", "onFind", "each:Ana", "post:FIND",
                "pre:EXISTS", "onExists:true:1", "post:EXISTS",
                "pre:EXISTS", "onExists:false:9", "post:EXISTS",
                "pre:COUNT", "onCount:1", "post:COUNT"),
            calls
        );
    }

    @Test
    @DisplayName("list y page avisan por cada entidad")
    void listAndPageFlows() {
        people.create(new PersonInput("Ana", 30));
        calls.clear();

        people.list(null, "age>20", Sort.unsorted());
        people.page(null, "age>20", Pagination.unpaginated(), Sort.unsorted());

        assertIterableEquals(
            List.of("pre:LIST", "each:Ana", "post:LIST",
                "pre:PAGE", "onPage", "each:Ana", "post:PAGE"),
            calls
        );
    }

    @Test
    @DisplayName("stream avisa por cada entidad al consumirlo y cierra con postProcess al cerrarlo")
    void streamFlow() {
        people.create(new PersonInput("Ana", 30));
        calls.clear();

        try (var stream = people.stream(null, null, Sort.unsorted())) {
            assertIterableEquals(List.of("pre:STREAM"), calls);
            stream.toList();
        }

        assertIterableEquals(List.of("pre:STREAM", "each:Ana", "post:STREAM"), calls);
    }

    @Test
    @DisplayName("si el mapeo del input falla en create, no se persiste nada ni se llega a los eventos de después")
    void failingCreateStopsTheFlow() {
        var failing = new RecordingProvider(events) {
            @Override
            protected void mapInput(PersonInput input, Person entity, boolean isNew) {
                throw new IllegalArgumentException("invalid");
            }
        };

        assertThrows(IllegalArgumentException.class, () -> failing.create(new PersonInput("Ana", 30)));

        assertIterableEquals(List.of("pre:CREATE"), calls);
        assertEquals(0, failing.entities().size());
    }
}
