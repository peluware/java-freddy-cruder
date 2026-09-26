package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.ImportBinder;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportProblem;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Binds the cells of one {@link ExcelRow} to setters, by column index.
 */
public final class ExcelRowBinder {

    private final ExcelRow row;
    private final ImportBinder binder = new ImportBinder();

    /**
     * @param row the row being converted
     */
    public ExcelRowBinder(ExcelRow row) {
        this.row = row;
    }

    /**
     * @return the binder backing this row
     */
    public ImportBinder binder() {
        return binder;
    }

    /**
     * Builds the conversion of the row from the recorded problems.
     *
     * @param input   the input DTO
     * @param <INPUT> the input DTO type
     * @return {@code input} if no problem was recorded, the problems otherwise
     */
    public <INPUT> ImportConversion<INPUT> toConversion(INPUT input) {
        return binder.toConversion(input);
    }

    /**
     * Assigns the value of the cell at {@code column}, or records a problem if it is missing.
     * A failure of {@code accessor} is recorded as a problem too.
     *
     * @param setter   assigns the value
     * @param column   the 0-based column to read
     * @param field    the field path of the problem; it does not select the cell
     * @param accessor reads the value from the cell
     * @param message  the problem if the value is missing
     * @param <T>      the type of the value
     */
    public <T> void required(Consumer<? super T> setter, int column, String field, Function<ExcelCell, @Nullable T> accessor, String message) {
        required(setter, column, field, accessor, message, e -> ImportProblem.from(field, e));
    }

    /**
     * Assigns the value of the cell at {@code column}, or records a problem if it is missing.
     *
     * @param setter    assigns the value
     * @param column    the 0-based column to read
     * @param field     the field path of the problem; it does not select the cell
     * @param accessor  reads the value from the cell
     * @param message   the problem if the value is missing
     * @param onFailure turns a failure of the accessor into the problem to record
     * @param <T>       the type of the value
     */
    public <T> void required(
        Consumer<? super T> setter,
        int column,
        String field,
        Function<ExcelCell, @Nullable T> accessor,
        String message,
        Function<? super RuntimeException, ImportProblem> onFailure
    ) {
        binder.required(setter, () -> accessor.apply(row.cell(column)), field, message, onFailure);
    }

    /**
     * Assigns the value of the cell at {@code column} if it is present.
     * A failure of {@code accessor} is recorded as a problem.
     *
     * @param setter   assigns the value
     * @param column   the 0-based column to read
     * @param field    the field path of the problem; it does not select the cell
     * @param accessor reads the value from the cell
     * @param <T>      the type of the value
     */
    public <T> void optional(Consumer<? super T> setter, int column, String field, Function<ExcelCell, @Nullable T> accessor) {
        optional(setter, column, field, accessor, e -> ImportProblem.from(field, e));
    }

    /**
     * Assigns the value of the cell at {@code column} if it is present.
     *
     * @param setter    assigns the value
     * @param column    the 0-based column to read
     * @param field     the field path of the problem; it does not select the cell
     * @param accessor  reads the value from the cell
     * @param onFailure turns a failure of the accessor into the problem to record
     * @param <T>       the type of the value
     */
    public <T> void optional(
        Consumer<? super T> setter,
        int column,
        String field,
        Function<ExcelCell, @Nullable T> accessor,
        Function<? super RuntimeException, ImportProblem> onFailure
    ) {
        binder.optional(setter, () -> accessor.apply(row.cell(column)), onFailure);
    }
}
