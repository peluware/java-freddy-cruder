package com.peluware.freddy.cruder.bulkimport.excel;

/**
 * Thrown when the file has no sheet with the expected name.
 */
public class ExcelSheetMissingException extends RuntimeException {

    private final String sheetName;

    /**
     * @param sheetName the name of the sheet that was expected
     */
    public ExcelSheetMissingException(String sheetName) {
        super("The file has no sheet named '" + sheetName + "'");
        this.sheetName = sheetName;
    }

    public String sheetName() {
        return sheetName;
    }
}
