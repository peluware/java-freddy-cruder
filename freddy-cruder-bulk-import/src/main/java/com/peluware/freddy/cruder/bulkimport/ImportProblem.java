package com.peluware.freddy.cruder.bulkimport;

import org.jspecify.annotations.Nullable;

/**
 * Something wrong with a record.
 *
 * @param message what is wrong, as shown to the user
 * @param field   the path of the field it applies to, or {@code null} if it concerns the whole record
 */
public record ImportProblem(String message, @Nullable String field) {

    /**
     * Creates a record-level problem from a failure.
     *
     * @param failure what went wrong
     * @return the problem; the message of the failure, or its type if it has none
     */
    public static ImportProblem from(Throwable failure) {
        var message = failure.getMessage();
        return of(message != null ? message : failure.getClass().getSimpleName());
    }

    /**
     * Creates a field-level problem from a failure.
     *
     * @param field   the path of the field
     * @param failure what went wrong
     * @return the problem; the message of the failure, or its type if it has none
     */
    public static ImportProblem from(String field, Throwable failure) {
        var message = failure.getMessage();
        return of(field, message != null ? message : failure.getClass().getSimpleName());
    }

    /**
     * Creates a record-level problem.
     *
     * @param message what is wrong
     * @return the problem
     */
    public static ImportProblem of(String message) {
        return new ImportProblem(message, null);
    }

    /**
     * Creates a field-level problem.
     *
     * @param field   the path of the field
     * @param message what is wrong
     * @return the problem
     */
    public static ImportProblem of(String field, String message) {
        return new ImportProblem(message, field);
    }
}
