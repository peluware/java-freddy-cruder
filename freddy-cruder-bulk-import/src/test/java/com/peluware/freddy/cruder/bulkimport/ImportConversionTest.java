package com.peluware.freddy.cruder.bulkimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportConversionTest {

    @Test
    void aSkippedConversionCarriesItsReason() {
        var conversion = ImportConversion.<String>skipped("Ya existe");

        assertEquals(new ImportConversion.Skipped<String>("Ya existe"), conversion);
    }

    @Test
    void aSkippedConversionNeedsAReason() {
        assertThrows(IllegalArgumentException.class, () -> ImportConversion.skipped(" "));
    }

    @Test
    void attemptLeavesASkippedConversionAlone() {
        var conversion = ImportConversion.attempt(() -> ImportConversion.<String>skipped("Ya existe"));

        assertEquals(new ImportConversion.Skipped<String>("Ya existe"), conversion);
    }

    @Test
    void attemptGivesWhatTheConversionReturned() {
        var conversion = ImportConversion.attempt(() -> ImportConversion.converted("Ana"));

        assertEquals(new ImportConversion.Converted<>("Ana"), conversion);
    }

    @Test
    void attemptTurnsAFailureIntoAnUnconvertibleRecordWorkedWithItsMessage() {
        var conversion = ImportConversion.<String>attempt(() -> {
            throw new IllegalArgumentException("Edad no válida");
        });

        var unconvertible = assertInstanceOf(ImportConversion.Unconvertible.class, conversion);
        assertEquals(List.of(ImportProblem.of("Edad no válida")), unconvertible.problems());
    }

    @Test
    void attemptWordsAFailureWithoutMessageWithItsType() {
        var conversion = ImportConversion.<String>attempt(() -> {
            throw new IllegalStateException();
        });

        var unconvertible = assertInstanceOf(ImportConversion.Unconvertible.class, conversion);
        assertEquals(List.of(ImportProblem.of("IllegalStateException")), unconvertible.problems());
    }
}
