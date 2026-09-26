package com.peluware.freddy.cruder.bulkimport;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * The result of converting a record: an input DTO, the problems that prevent it, or nothing to do
 * with it.
 *
 * @param <INPUT> the input DTO type used to create the resource
 */
public sealed interface ImportConversion<INPUT> {

    /**
     * The record was converted.
     *
     * @param input   the input DTO
     * @param <INPUT> the input DTO type
     */
    record Converted<INPUT>(INPUT input) implements ImportConversion<INPUT> {

        public Converted {
            Objects.requireNonNull(input, "Input must not be null");
        }
    }

    /**
     * The record could not be converted.
     *
     * @param problems what is wrong with it; never empty
     * @param <INPUT>  the input DTO type
     */
    record Unconvertible<INPUT>(List<ImportProblem> problems) implements ImportConversion<INPUT> {

        public Unconvertible {
            if (problems.isEmpty()) {
                throw new IllegalArgumentException("An unconvertible record needs at least one problem");
            }
            problems = List.copyOf(problems);
        }
    }

    /**
     * The record is left alone: there is nothing to do with it, and nothing wrong with it.
     *
     * @param reason  why it is left alone
     * @param <INPUT> the input DTO type
     */
    record Skipped<INPUT>(String reason) implements ImportConversion<INPUT> {

        public Skipped {
            Objects.requireNonNull(reason, "Reason must not be null");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("A skipped record needs a reason");
            }
        }
    }

    /**
     * Creates the conversion of a converted record.
     *
     * @param input   the input DTO
     * @param <INPUT> the input DTO type
     * @return the conversion holding {@code input}
     */
    static <INPUT> ImportConversion<INPUT> converted(INPUT input) {
        return new Converted<>(input);
    }

    /**
     * Creates the conversion of a record that failed with one problem.
     *
     * @param problem what is wrong with the record
     * @param <INPUT> the input DTO type
     * @return the unconvertible conversion
     */
    static <INPUT> ImportConversion<INPUT> unconvertible(ImportProblem problem) {
        return new Unconvertible<>(List.of(problem));
    }

    /**
     * Creates the conversion of a record that failed with several problems.
     *
     * @param problems what is wrong with the record; not empty
     * @param <INPUT>  the input DTO type
     * @return the unconvertible conversion
     */
    static <INPUT> ImportConversion<INPUT> unconvertible(List<ImportProblem> problems) {
        return new Unconvertible<>(problems);
    }

    /**
     * Creates the conversion of a record that is left alone.
     *
     * @param reason  why the record is left alone; must not be blank
     * @param <INPUT> the input DTO type
     * @return the skipped conversion
     */
    static <INPUT> ImportConversion<INPUT> skipped(String reason) {
        return new Skipped<>(reason);
    }

    /**
     * Runs a conversion, turning a failure into an unconvertible record.
     *
     * @param conversion the conversion to run
     * @param <INPUT>    the input DTO type
     * @return the conversion, or the unconvertible record its failure became
     */
    static <INPUT> ImportConversion<INPUT> attempt(Supplier<? extends ImportConversion<INPUT>> conversion) {
        try {
            return conversion.get();
        } catch (RuntimeException e) {
            return unconvertible(ImportProblem.from(e));
        }
    }
}
