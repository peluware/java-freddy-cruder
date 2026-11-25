package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

class JpaQueryHintsTest {

    private final List<String> events = new ArrayList<>();
    private final Map<String, Object> applied = new LinkedHashMap<>();

    /** Una consulta que solo anota cuándo se materializa su resultado. */
    private final JpaQuery<Object, Object, String> base = new JpaQuery<>() {
        @Override
        public CriteriaQuery<Object> create(CriteriaBuilder cb) {
            throw new UnsupportedOperationException();
        }

        @Override
        public From<?, Object> from(CriteriaQuery<Object> cq) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Selection<?> select(From<?, Object> from, CriteriaBuilder cb) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @Nullable Predicate build(From<?, Object> from, CriteriaBuilder cb) {
            return null;
        }

        @Override
        public String result(TypedQuery<Object> query) {
            events.add("result");
            return "done";
        }
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
    @DisplayName("fija los hints en la consulta antes de que el delegado materialice el resultado")
    void setsHintsBeforeTheDelegateResult() {
        var query = new JpaQueryHints<>(base, Map.of("a", 1));

        assertEquals("done", query.result(typedQuery()));
        assertIterableEquals(List.of("hint:a", "result"), events);
        assertEquals(Map.of("a", 1), applied);
    }

    @Test
    @DisplayName("copia el mapa: cambiarlo después no altera los hints de la consulta")
    void copiesTheHintsMap() {
        var hints = new LinkedHashMap<String, Object>();
        hints.put("a", 1);
        var query = new JpaQueryHints<>(base, hints);

        hints.put("b", 2);
        query.result(typedQuery());

        assertEquals(Map.of("a", 1), applied);
    }

    @Test
    @DisplayName("envolver una consulta ya envuelta aplica los hints de ambas")
    void nestedWrappersApplyBothSets() {
        var query = new JpaQueryHints<>(new JpaQueryHints<>(base, Map.of("inner", 1)), Map.of("outer", 2));

        query.result(typedQuery());

        assertEquals(Map.of("inner", 1, "outer", 2), applied);
        assertEquals(1, events.stream().filter("result"::equals).count());
    }
}
