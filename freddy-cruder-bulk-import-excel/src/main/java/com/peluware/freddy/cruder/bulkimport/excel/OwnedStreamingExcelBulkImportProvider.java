package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.OwnedCreateProvider;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;

/**
 * Reads a sheet of an {@code .xlsx} file row by row without loading the file into memory.
 * Formulas are read as the result saved with them, and the raw POI views only move forward.
 * Requires excel-streaming-reader on the classpath.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <INPUT>    the input DTO type used to create the resource
 * @param <OUTPUT>   the output DTO or projection type returned by the provider
 */
public abstract class OwnedStreamingExcelBulkImportProvider<OWNER_ID, INPUT, OUTPUT> extends AbstractOwnedExcelBulkImportProvider<OWNER_ID, INPUT, OUTPUT> {

    /**
     * @param createProvider the provider that creates each record
     */
    protected OwnedStreamingExcelBulkImportProvider(OwnedCreateProvider<OWNER_ID, INPUT, OUTPUT> createProvider) {
        super(createProvider);
    }

    @Override
    protected Workbook openWorkbook(InputStream in) {
        return StreamingWorkbooks.open(in);
    }

    @Override
    protected @Nullable FormulaEvaluator evaluatorOf(Workbook workbook) {
        return null;
    }
}
