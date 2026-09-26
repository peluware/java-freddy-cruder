package com.peluware.freddy.cruder.bulkimport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportBinderTest {

    @Test
    void requiredAssignsThePresentValue() {
        var input = new AccountInput();
        var binder = new ImportBinder();

        binder.required(input::setCode, "1.1", "code", "Code is required");

        assertEquals("1.1", input.code);
        assertFalse(binder.hasProblems());
    }

    @Test
    void requiredRecordsAProblemForTheFieldWhenTheValueIsMissing() {
        var input = new AccountInput();
        var binder = new ImportBinder();

        binder.required(input::setCode, (String) null, "code", "Code is required");

        assertNull(input.code);
        assertEquals(new ImportProblem("Code is required", "code"), binder.problems().getFirst());
    }

    @Test
    void optionalDoesNothingWhenTheValueIsMissing() {
        var input = new AccountInput();
        var binder = new ImportBinder();

        binder.optional(input::setLevel, (Integer) null);

        assertNull(input.level);
        assertFalse(binder.hasProblems());
    }

    @Test
    void anAccessorThatThrowsBecomesTheProblemGivenByOnFailure() {
        var input = new AccountInput();
        var binder = new ImportBinder();

        binder.required(
            input::setLevel,
            () -> Integer.parseInt("not a number"),
            "level", "Level is required",
            e -> ImportProblem.of("level", "Level must be a number")
        );

        assertNull(input.level);
        assertEquals("Level must be a number", binder.problems().getFirst().message());
    }

    @Test
    void toConversionGivesTheInputOrTheProblems() {
        var input = new AccountInput();
        var clean = new ImportBinder();
        var broken = new ImportBinder();
        broken.reject("code", "Code is wrong");

        assertInstanceOf(ImportConversion.Converted.class, clean.toConversion(input));
        var unconvertible = assertInstanceOf(ImportConversion.Unconvertible.class, broken.toConversion(input));
        assertTrue(unconvertible.problems().contains(new ImportProblem("Code is wrong", "code")));
    }
}
