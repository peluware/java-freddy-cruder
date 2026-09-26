package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.bulkimport.ImportBinder;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportProblem;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Binds the columns of a {@link CsvRow} to setters, by column index.
 */
public final class CsvRowBinder {

    private final CsvRow row;
    private final ImportBinder binder = new ImportBinder();

    /**
     * @param row the row being converted
     */
    public CsvRowBinder(CsvRow row) {
        this.row = row;
    }

    /**
     * @return the binder backing this row
     */
    public ImportBinder binder() {
        return binder;
    }

    /**
     * Builds the conversion of the row from the input and the recorded problems.
     *
     * @param input   the input DTO
     * @param <INPUT> the input DTO type
     * @return the input if no problem was recorded, the problems otherwise
     */
    public <INPUT> ImportConversion<INPUT> toConversion(INPUT input) {
        return binder.toConversion(input);
    }

    /**
     * Assigns the value of a column, or records a problem if it is missing or the accessor fails.
     *
     * @param setter   assigns the value
     * @param column   the column to read, starting at {@code 0}
     * @param field    the path of the field, for the problem
     * @param accessor reads the value from the column
     * @param message  the problem if the value is missing
     * @param <T>      the type of the value
     */
    public <T> void required(Consumer<? super T> setter, int column, String field, Function<CsvCell, @Nullable T> accessor, String message) {
        required(setter, column, field, accessor, message, e -> ImportProblem.from(field, e));
    }

    /**
     * Assigns the value of a column, or records a problem if it is missing or the accessor fails.
     *
     * @param setter    assigns the value
     * @param column    the column to read, starting at {@code 0}
     * @param field     the path of the field, for the problem
     * @param accessor  reads the value from the column
     * @param message   the problem if the value is missing
     * @param onFailure turns a failure of the accessor into the problem to record
     * @param <T>       the type of the value
     */
    public <T> void required(
        Consumer<? super T> setter,
        int column,
        String field,
        Function<CsvCell, @Nullable T> accessor,
        String message,
        Function<? super RuntimeException, ImportProblem> onFailure
    ) {
        binder.required(setter, () -> accessor.apply(row.cell(column)), field, message, onFailure);
    }

    /**
     * Assigns the value of a column if it is present, or records a problem if the accessor fails.
     *
     * @param setter   assigns the value
     * @param column   the column to read, starting at {@code 0}
     * @param field    the path of the field, for the problem
     * @param accessor reads the value from the column
     * @param <T>      the type of the value
     */
    public <T> void optional(Consumer<? super T> setter, int column, String field, Function<CsvCell, @Nullable T> accessor) {
        optional(setter, column, field, accessor, e -> ImportProblem.from(field, e));
    }

    /**
     * Assigns the value of a column if it is present, or records a problem if the accessor fails.
     *
     * @param setter    assigns the value
     * @param column    the column to read, starting at {@code 0}
     * @param field     the path of the field, for the problem
     * @param accessor  reads the value from the column
     * @param onFailure turns a failure of the accessor into the problem to record
     * @param <T>       the type of the value
     */
    public <T> void optional(
        Consumer<? super T> setter,
        int column,
        String field,
        Function<CsvCell, @Nullable T> accessor,
        Function<? super RuntimeException, ImportProblem> onFailure
    ) {
        binder.optional(setter, () -> accessor.apply(row.cell(column)), onFailure);
    }
}
