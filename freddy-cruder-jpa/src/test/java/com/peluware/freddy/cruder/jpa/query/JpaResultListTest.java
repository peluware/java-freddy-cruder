package com.peluware.freddy.cruder.jpa.query;

import com.peluware.domain.Pagination;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JpaResultListTest {

    private final List<String> calls = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private TypedQuery<Object> typedQuery() {
        var holder = new Object[1];
        holder[0] = Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{TypedQuery.class},
            (_, method, args) -> switch (method.getName()) {
                case "setFirstResult", "setMaxResults" -> {
                    calls.add(method.getName() + ":" + args[0]);
                    yield holder[0];
                }
                case "getResultList" -> List.of("row");
                default -> throw new UnsupportedOperationException(method.getName());
            }
        );
        return (TypedQuery<Object>) holder[0];
    }

    @Test
    @DisplayName("una página pide su desplazamiento y su tamaño")
    void paginatedListSetsOffsetAndSize() {
        var rows = JpaResult.<Object>list(Pagination.of(3, 20)).result(typedQuery());

        assertEquals(List.of("row"), rows);
        assertIterableEquals(List.of("setFirstResult:60", "setMaxResults:20"), calls);
    }

    @Test
    @DisplayName("sin paginar no limita nada")
    void unpaginatedListSetsNothing() {
        JpaResult.<Object>list().result(typedQuery());

        assertIterableEquals(List.of(), calls);
    }

    @Test
    @DisplayName("un desplazamiento que no cabe en int falla en vez de volverse negativo")
    void overflowingOffsetFails() {
        var pagination = Pagination.of(Integer.MAX_VALUE, 20);

        assertThrows(ArithmeticException.class, () -> JpaResult.<Object>list(pagination).result(typedQuery()));
        assertIterableEquals(List.of(), calls);
    }
}
