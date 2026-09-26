package com.peluware.freddy.cruder.bulkimport.csv;

import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * A record of a CSV file, read by 0-based column index.
 */
public final class CsvRow {

    private final CSVRecord record;
    private final int positionOffset;
    private final int firstColumn;

    CsvRow(CSVRecord record, int positionOffset, int firstColumn) {
        this.record = record;
        this.positionOffset = positionOffset;
        this.firstColumn = firstColumn;
    }

    /**
     * @return the number of the record, counting the header record if there is one
     */
    public int number() {
        return (int) record.getRecordNumber() + positionOffset;
    }

    /**
     * Returns the value at a column, counted from column 0 of the record.
     *
     * @param column the 0-based column
     * @return the value at that column, blank if the record is shorter
     */
    public CsvCell cell(int column) {
        return new CsvCell(textAt(column));
    }

    /**
     * @param columns how many columns to read, starting at the source's first column
     * @return the text of each of them
     */
    public List<String> values(int columns) {
        var values = new ArrayList<String>(columns);
        for (var column = 0; column < columns; column++) {
            values.add(textAt(firstColumn + column));
        }
        return values;
    }

    /**
     * @return how many columns the record has, from the source's first column
     */
    public int size() {
        return Math.max(record.size() - firstColumn, 0);
    }

    /**
     * @return whether every column of the record is empty, from the source's first column
     */
    public boolean blank() {
        for (var column = 0; column < size(); column++) {
            if (!textAt(firstColumn + column).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the underlying Apache Commons CSV record
     */
    public CSVRecord raw() {
        return record;
    }

    private String textAt(int column) {
        if (column < 0 || column >= record.size()) {
            return "";
        }
        return record.get(column).replace(' ', ' ').replace(' ', ' ').trim();
    }
}
