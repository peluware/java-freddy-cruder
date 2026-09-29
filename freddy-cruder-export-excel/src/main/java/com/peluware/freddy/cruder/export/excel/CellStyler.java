package com.peluware.freddy.cruder.export.excel;

import org.apache.poi.ss.usermodel.Cell;

/**
 * Called for every cell {@link ExcelWriting} writes, right after its value is set. Apply whatever
 * {@link org.apache.poi.ss.usermodel.CellStyle} you build yourself — freddy-cruder has no opinion
 * on fonts, colors or borders.
 */
@FunctionalInterface
public interface CellStyler {

    /**
     * Styles nothing.
     */
    CellStyler NONE = (_, _, _, _) -> {
    };

    /**
     * @param cell        the cell that was just written
     * @param rowIndex    the cell's row, absolute in the sheet
     * @param columnIndex the cell's column, {@code 0}-based, relative to the table (not the sheet)
     * @param header      {@code true} for a field-label cell, {@code false} for a record's cell
     */
    void style(Cell cell, int rowIndex, int columnIndex, boolean header);
}
