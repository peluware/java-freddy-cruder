package com.peluware.freddy.cruder.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReflectUtilsTest {

    static abstract class Base<A, B> {
    }

    static class Direct extends Base<String, Integer> {
    }

    static abstract class Middle<X> extends Base<X, Long> {
    }

    static class Indirect extends Middle<Double> {
    }

    static class Parameterized extends Base<List<String>, Integer> {
    }

    static class Unrelated {
    }

    @Test
    @DisplayName("resuelve el argumento de tipo declarado directamente en la superclase")
    void resolvesDirectArguments() {
        assertEquals(String.class, ReflectUtils.resolveGenericType(Direct.class, Base.class, 0));
        assertEquals(Integer.class, ReflectUtils.resolveGenericType(Direct.class, Base.class, 1));
    }

    @Test
    @DisplayName("resuelve un argumento que llega a través de una clase intermedia genérica")
    void resolvesThroughAnIntermediateClass() {
        assertEquals(Double.class, ReflectUtils.resolveGenericType(Indirect.class, Base.class, 0));
        assertEquals(Long.class, ReflectUtils.resolveGenericType(Indirect.class, Base.class, 1));
    }

    @Test
    @DisplayName("un argumento parametrizado se resuelve a su tipo crudo")
    void parameterizedArgumentResolvesToItsRawType() {
        assertEquals(List.class, ReflectUtils.resolveGenericType(Parameterized.class, Base.class, 0));
    }

    @Test
    @DisplayName("si la clase no extiende el objetivo, falla nombrándolo")
    void failsWhenTheTargetIsNotASuperclass() {
        var error = assertThrows(IllegalStateException.class, () -> ReflectUtils.resolveGenericType(Unrelated.class, (Class) Base.class, 0));

        assertEquals("Target generic superclass not found: " + Base.class.getName(), error.getMessage());
    }
}
