package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.CreateProvider;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.InputStream;

/**
 * Reads a sheet of an {@code .xls} or {@code .xlsx} file, loading the whole file into memory and
 * evaluating formulas.
 *
 * @param <INPUT>  the input DTO type used to create the resource
 * @param <OUTPUT> the output DTO or projection type returned by the provider
 */
public abstract class ExcelBulkImportProvider<INPUT, OUTPUT> extends AbstractExcelBulkImportProvider<INPUT, OUTPUT> {

    /**
     * @param createProvider the provider that creates each record
     */
    protected ExcelBulkImportProvider(CreateProvider<INPUT, OUTPUT> createProvider) {
        super(createProvider);
    }

    @Override
    protected Workbook openWorkbook(InputStream in) {
        return ClassicWorkbooks.open(in);
    }

    @Override
    protected FormulaEvaluator evaluatorOf(Workbook workbook) {
        return ClassicWorkbooks.evaluatorOf(workbook);
    }
}
