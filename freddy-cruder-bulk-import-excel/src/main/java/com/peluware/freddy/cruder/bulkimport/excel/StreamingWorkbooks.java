package com.peluware.freddy.cruder.bulkimport.excel;

import com.github.pjfanning.xlsx.StreamingReader;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.InputStream;

/**
 * Opens {@code .xlsx} workbooks row by row, without loading the file into memory.
 */
final class StreamingWorkbooks {

    private StreamingWorkbooks() {
    }

    static Workbook open(InputStream in) {
        return StreamingReader.builder().rowCacheSize(100).bufferSize(4096).open(in);
    }
}
