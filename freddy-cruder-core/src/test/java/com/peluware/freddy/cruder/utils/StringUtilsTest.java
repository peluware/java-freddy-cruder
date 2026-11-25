package com.peluware.freddy.cruder.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringUtilsTest {

    @Test
    @DisplayName("isEmpty es verdadero para nulo y cadena vacía, y falso para espacios")
    void isEmpty() {
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertFalse(StringUtils.isEmpty(" "));
    }

    @Test
    @DisplayName("isBlank es verdadero para nulo, vacío y solo espacios")
    void isBlank() {
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank("  \t "));
        assertFalse(StringUtils.isBlank(" a "));
    }

    @Test
    @DisplayName("normalize devuelve nulo para lo que está en blanco y recorta el resto")
    void normalize() {
        assertNull(StringUtils.normalize(null));
        assertNull(StringUtils.normalize("   "));
        assertEquals("ana", StringUtils.normalize("  ana "));
    }
}
