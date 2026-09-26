package com.peluware.freddy.cruder.bulkimport;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Thrown when a record fails during a bulk import, carrying its position, its problems and the original failure.
 */
public class BulkImportRecordException extends RuntimeException {

    private final int position;
    private final transient List<ImportProblem> problems;

    /**
     * A record the source could not convert.
     *
     * @param position the position of the failing record in its origin
     * @param problems what the source found wrong with it
     */
    public BulkImportRecordException(int position, List<ImportProblem> problems) {
        this(position, problems, null);
    }

    /**
     * A record whose processing failed.
     *
     * @param position the position of the failing record in its origin
     * @param cause    the original failure
     */
    public BulkImportRecordException(int position, Throwable cause) {
        this(position, List.of(), cause);
    }

    /**
     * A record that failed, with problems and an optional cause.
     *
     * @param position the position of the failing record in its origin
     * @param problems what is wrong with it; may be empty
     * @param cause    the original failure, or {@code null} if there was none
     */
    public BulkImportRecordException(int position, List<ImportProblem> problems, @Nullable Throwable cause) {
        super(message(problems, cause), cause);
        this.position = position;
        this.problems = List.copyOf(problems);
    }

    /**
     * @return the position of the failing record in its origin
     */
    public int position() {
        return position;
    }

    /**
     * @return what is wrong with the record; empty if nobody said
     */
    public List<ImportProblem> problems() {
        return problems;
    }

    private static @Nullable String message(List<ImportProblem> problems, @Nullable Throwable cause) {
        if (!problems.isEmpty()) {
            return problems.getFirst().message();
        }
        return cause != null ? cause.getMessage() : "The record could not be processed";
    }
}
