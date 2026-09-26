package com.peluware.freddy.cruder.bulkimport.excel;

import java.util.List;

/**
 * Metadata of an Excel sheet.
 *
 * @param headers the texts of the header row, one per column
 */
public record ExcelMetadata(List<String> headers) {
}
