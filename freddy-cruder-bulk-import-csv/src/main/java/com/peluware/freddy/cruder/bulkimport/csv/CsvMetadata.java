package com.peluware.freddy.cruder.bulkimport.csv;

import java.util.List;

/**
 * The metadata of a CSV file.
 *
 * @param headers the texts of the header record, one per column
 */
public record CsvMetadata(List<String> headers) {
}
