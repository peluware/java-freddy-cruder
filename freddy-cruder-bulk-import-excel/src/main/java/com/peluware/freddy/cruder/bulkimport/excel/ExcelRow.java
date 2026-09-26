package com.peluware.freddy.cruder.bulkimport.excel;

import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A row of a sheet, with its cells reached by 0-based column index.
 */
public final class ExcelRow {

    private final Row row;
    private final @Nullable FormulaEvaluator evaluator;
    private final boolean date1904;
    private final int firstColumn;

    ExcelRow(Row row, @Nullable FormulaEvaluator evaluator, boolean date1904, int firstColumn) {
        this.row = row;
        this.evaluator = evaluator;
        this.date1904 = date1904;
        this.firstColumn = firstColumn;
    }

    /**
     * @return the 1-based row number, as a spreadsheet shows it
     */
    public int number() {
        return row.getRowNum() + 1;
    }

    /**
     * Returns the cell at a column, counted from column 0 of the sheet.
     *
     * @param column the 0-based column
     * @return the cell, blank if the row does not reach it
     */
    public ExcelCell cell(int column) {
        return new ExcelCell(column < 0 ? null : row.getCell(column), evaluator, date1904);
    }

    /**
     * Returns the text of the first columns of the row, from the layout's first column.
     *
     * @param columns how many columns to read
     * @return the text of each column
     */
    public List<String> values(int columns) {
        var values = new ArrayList<String>(columns);
        for (var column = 0; column < columns; column++) {
            values.add(cell(firstColumn + column).text());
        }
        return values;
    }

    /**
     * @return how many columns the row reaches, from the layout's first column
     */
    public int size() {
        return Math.max(row.getLastCellNum() - firstColumn, 0);
    }

    /**
     * @return whether every cell is empty, from the layout's first column
     */
    public boolean blank() {
        for (var column = 0; column < size(); column++) {
            if (!cell(firstColumn + column).blank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the underlying Apache POI row
     */
    public Row raw() {
        return row;
    }

    /**
     * @return the formula evaluator, or {@code null} if formulas are read as the result saved with them
     */
    public @Nullable FormulaEvaluator evaluator() {
        return evaluator;
    }
}
