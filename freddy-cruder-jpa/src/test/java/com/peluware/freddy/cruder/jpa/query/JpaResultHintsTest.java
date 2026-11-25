package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class JpaResultHintsTest {

    private final List<String> events = new ArrayList<>();
    private final Map<String, Object> applied = new LinkedHashMap<>();

    private final JpaResult<Object, String> base = _ -> {
        events.add("result");
        return "done";
    };

    @SuppressWarnings("unchecked")
    private TypedQuery<Object> typedQuery() {
        return (TypedQuery<Object>) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{TypedQuery.class},
            (_, method, args) -> {
                if (method.getName().equals("setHint")) {
                    applied.put((String) args[0], args[1]);
                    events.add("hint:" + args[0]);
                    return null;
                }
                throw new UnsupportedOperationException(method.getName());
            }
        );
    }

    @Test
    @DisplayName("fija los hints en la consulta antes de materializar el resultado")
    void setsHintsBeforeTheResult() {
        var result = new JpaResultHints<>(base, Map.of("a", 1));

        assertEquals("done", result.result(typedQuery()));
        assertIterableEquals(List.of("hint:a", "result"), events);
        assertEquals(Map.of("a", 1), applied);
    }

    @Test
    @DisplayName("copia el mapa: cambiarlo después no altera los hints del resultado")
    void copiesTheHintsMap() {
        var hints = new LinkedHashMap<String, Object>();
        hints.put("a", 1);
        var result = new JpaResultHints<>(base, hints);

        hints.put("b", 2);
        result.result(typedQuery());

        assertEquals(Map.of("a", 1), applied);
    }

    @Test
    @DisplayName("envolver un resultado ya envuelto aplica los hints de ambos")
    void nestedWrappersApplyBothSets() {
        var result = new JpaResultHints<>(new JpaResultHints<>(base, Map.of("inner", 1)), Map.of("outer", 2));

        result.result(typedQuery());

        assertEquals(Map.of("inner", 1, "outer", 2), applied);
        assertEquals(1, events.stream().filter("result"::equals).count());
    }
}
