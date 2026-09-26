package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.CreateProvider;
import com.peluware.freddy.cruder.bulkimport.BulkImportPreview;
import com.peluware.freddy.cruder.bulkimport.BulkImportProvider;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Base of the Excel bulk import providers: reads one sheet forward, skipping blank rows, and
 * converts each row into a record.
 *
 * @param <INPUT>  the input DTO type used to create the resource
 * @param <OUTPUT> the output DTO or projection type returned by the provider
 */
public abstract class AbstractExcelBulkImportProvider<INPUT, OUTPUT> extends BulkImportProvider<INPUT, List<String>, ExcelMetadata, OUTPUT> {

    AbstractExcelBulkImportProvider(CreateProvider<INPUT, OUTPUT> createProvider) {
        super(createProvider);
    }

    /**
     * @return the name of the sheet to read
     */
    protected abstract String sheetName();

    /**
     * @return where the table sits in the sheet; {@link ExcelLayout#headerFirst()} by default
     */
    protected ExcelLayout layout() {
        return ExcelLayout.headerFirst();
    }

    /**
     * Converts a row into an input DTO or the problems that prevent it; a thrown exception becomes a problem.
     *
     * @param row the row
     * @return the conversion of the record
     */
    protected abstract ImportConversion<INPUT> convert(ExcelRow row);

    /**
     * Opens the workbook from the file.
     *
     * @param in the file; the provider does not close it
     * @return the workbook, which the provider closes
     */
    protected abstract Workbook openWorkbook(InputStream in);

    /**
     * Provides the evaluator used to read formulas.
     *
     * @param workbook the workbook returned by {@link #openWorkbook}
     * @return the evaluator, or {@code null} to read formulas as the result saved with them
     */
    protected abstract @Nullable FormulaEvaluator evaluatorOf(Workbook workbook);

    @Override
    public BulkImportResult execute(InputStream in) {
        try (var workbook = openWorkbook(in)) {
            return ExcelSheetImport.execute(workbook, evaluatorOf(workbook), sheetName(), layout(), hooks());
        } catch (IOException e) {
            throw new UncheckedIOException("The file could not be closed", e);
        }
    }

    @Override
    public BulkImportPreview<List<String>, ExcelMetadata> preview(InputStream in) {
        try (var workbook = openWorkbook(in)) {
            return ExcelSheetImport.preview(workbook, evaluatorOf(workbook), sheetName(), layout(), hooks());
        } catch (IOException e) {
            throw new UncheckedIOException("The file could not be closed", e);
        }
    }

    private ExcelSheetImport.Hooks<INPUT, OUTPUT> hooks() {
        return new ExcelSheetImport.Hooks<>(this::convert, createProvider::create, this::created, this::createFailed);
    }
}
