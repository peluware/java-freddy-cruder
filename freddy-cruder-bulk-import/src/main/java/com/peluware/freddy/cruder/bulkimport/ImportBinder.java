package com.peluware.freddy.cruder.bulkimport;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Binds record fields to setters, collecting a problem for each field that could not be bound.
 */
public final class ImportBinder {

    private final List<ImportProblem> problems = new ArrayList<>();

    /**
     * Assigns a value if it is there, or records a problem for it.
     *
     * @param setter  assigns the value
     * @param value   the value
     * @param field   the path of the field it came from
     * @param message what is wrong if the value is missing
     * @param <T>     the type of the value
     */
    public <T> void required(Consumer<? super T> setter, @Nullable T value, String field, String message) {
        if (value == null) {
            problems.add(ImportProblem.of(field, message));
        } else {
            setter.accept(value);
        }
    }

    /**
     * Assigns the value read by an accessor if it is there, or records a problem; a failure of the
     * accessor is recorded through {@code onFailure}.
     *
     * @param setter    assigns the value
     * @param accessor  reads the value
     * @param field     the path of the field it came from
     * @param message   what is wrong if the value is missing
     * @param onFailure turns a failure of the accessor into the problem to record
     * @param <T>       the type of the value
     */
    public <T> void required(
        Consumer<? super T> setter,
        Supplier<@Nullable T> accessor,
        String field,
        String message,
        Function<? super RuntimeException, ImportProblem> onFailure
    ) {
        T value;
        try {
            value = accessor.get();
        } catch (RuntimeException e) {
            problems.add(onFailure.apply(e));
            return;
        }
        required(setter, value, field, message);
    }

    /**
     * Assigns a value if it is there; a missing value records no problem.
     *
     * @param setter assigns the value
     * @param value  the value
     * @param <T>    the type of the value
     */
    public <T> void optional(Consumer<? super T> setter, @Nullable T value) {
        if (value != null) {
            setter.accept(value);
        }
    }

    /**
     * Assigns the value read by an accessor if it is there; a failure of the accessor is recorded
     * through {@code onFailure}.
     *
     * @param setter    assigns the value
     * @param accessor  reads the value
     * @param onFailure turns a failure of the accessor into the problem to record
     * @param <T>       the type of the value
     */
    public <T> void optional(Consumer<? super T> setter, Supplier<@Nullable T> accessor, Function<? super RuntimeException, ImportProblem> onFailure) {
        T value;
        try {
            value = accessor.get();
        } catch (RuntimeException e) {
            problems.add(onFailure.apply(e));
            return;
        }
        optional(setter, value);
    }

    /**
     * Records a problem that applies to one field.
     *
     * @param field   the path of the field
     * @param message what is wrong
     */
    public void reject(String field, String message) {
        problems.add(ImportProblem.of(field, message));
    }

    /**
     * @return whether any problem was recorded
     */
    public boolean hasProblems() {
        return !problems.isEmpty();
    }

    /**
     * @return the problems recorded so far
     */
    public List<ImportProblem> problems() {
        return List.copyOf(problems);
    }

    /**
     * Builds the conversion of a record.
     *
     * @param input   the input DTO, already assigned
     * @param <INPUT> the input DTO type
     * @return the conversion: {@code input} if nothing was recorded, the recorded problems otherwise
     */
    public <INPUT> ImportConversion<INPUT> toConversion(INPUT input) {
        return hasProblems() ? ImportConversion.unconvertible(problems()) : ImportConversion.converted(input);
    }
}
