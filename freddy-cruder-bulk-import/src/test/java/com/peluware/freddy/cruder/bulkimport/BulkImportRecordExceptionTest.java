package com.peluware.freddy.cruder.bulkimport;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BulkImportRecordExceptionTest {

    @Test
    void carriesThePositionAndTheProblemsOfARecordThatCouldNotBeConverted() {
        var problem = ImportProblem.of("code", "Code is required");

        var exception = new BulkImportRecordException(3, List.of(problem));

        assertEquals(3, exception.position());
        assertEquals(List.of(problem), exception.problems());
        assertEquals("Code is required", exception.getMessage());
    }

    @Test
    void carriesTheCauseOfARecordWhoseCreationFailed() {
        var cause = new IllegalStateException("Duplicate code");

        var exception = new BulkImportRecordException(5, cause);

        assertSame(cause, exception.getCause());
        assertTrue(exception.problems().isEmpty());
        assertEquals("Duplicate code", exception.getMessage());
    }

    @Test
    void anUnconvertibleRecordNeedsAtLeastOneProblem() {
        assertThrows(IllegalArgumentException.class, () -> ImportConversion.unconvertible(List.of()));
    }
}
