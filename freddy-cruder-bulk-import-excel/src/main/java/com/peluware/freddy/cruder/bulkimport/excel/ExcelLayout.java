package com.peluware.freddy.cruder.bulkimport.excel;

/**
 * Where the table of a sheet sits; rows are 1-based and columns 0-based.
 *
 * @param headerRow    the header row, or {@code 0} if the sheet has none
 * @param firstDataRow the first data row
 * @param firstColumn  the first column of the table; earlier columns are ignored
 */
public record ExcelLayout(int headerRow, int firstDataRow, int firstColumn) {

    public ExcelLayout {
        if (headerRow < 0 || firstDataRow < 1 || (headerRow > 0 && firstDataRow <= headerRow)) {
            throw new IllegalArgumentException("Invalid rows: header " + headerRow + ", first data row " + firstDataRow);
        }
        if (firstColumn < 0) {
            throw new IllegalArgumentException("Invalid first column: " + firstColumn);
        }
    }

    /**
     * Creates a layout with headers in row 1, data from row 2 and the table at column 0.
     *
     * @return the layout
     */
    public static ExcelLayout headerFirst() {
        return new ExcelLayout(1, 2, 0);
    }

    /**
     * Creates a layout without a header row and the table at column 0.
     *
     * @param firstDataRow the first data row
     * @return the layout
     */
    public static ExcelLayout noHeader(int firstDataRow) {
        return new ExcelLayout(0, firstDataRow, 0);
    }

    /**
     * Copies this layout with another first column.
     *
     * @param firstColumn the first column of the table
     * @return the new layout
     */
    public ExcelLayout withFirstColumn(int firstColumn) {
        return new ExcelLayout(headerRow, firstDataRow, firstColumn);
    }
}
