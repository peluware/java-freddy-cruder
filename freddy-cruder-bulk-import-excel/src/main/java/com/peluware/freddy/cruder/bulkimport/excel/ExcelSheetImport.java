package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.BulkImportPreview;
import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportRecordPreview;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Date1904Support;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reads a sheet forward, skipping blank rows, and imports or previews each row through the given hooks.
 */
final class ExcelSheetImport {

    /**
     * What is done with each row.
     *
     * @param convert      converts a row into an input DTO, its problems, or nothing to do
     * @param create       creates a record
     * @param created      called after a record is created
     * @param createFailed builds the exception thrown when creating a record fails
     * @param <INPUT>      the input DTO type
     * @param <OUTPUT>     the output type returned when creating
     */
    record Hooks<INPUT, OUTPUT>(
        Function<ExcelRow, ImportConversion<INPUT>> convert,
        Function<INPUT, OUTPUT> create,
        Consumer<OUTPUT> created,
        BiFunction<Integer, Exception, BulkImportRecordException> createFailed
    ) {
    }

    private ExcelSheetImport() {
    }

    static <INPUT, OUTPUT> BulkImportResult execute(
        Workbook workbook,
        @Nullable FormulaEvaluator evaluator,
        String sheetName,
        ExcelLayout layout,
        Hooks<INPUT, OUTPUT> hooks
    ) {
        var sheet = sheetOf(workbook, sheetName);
        var date1904 = isDate1904(workbook);
        var firstColumn = layout.firstColumn();
        var firstDataIndex = layout.firstDataRow() - 1;

        var created = 0;
        var skipped = 0;
        for (var raw : sheet) {
            if (raw.getRowNum() < firstDataIndex) {
                continue;
            }
            var row = new ExcelRow(raw, evaluator, date1904, firstColumn);
            if (row.blank()) {
                continue;
            }
            switch (ImportConversion.attempt(() -> hooks.convert().apply(row))) {
                case ImportConversion.Unconvertible<INPUT>(var problems) -> throw new BulkImportRecordException(row.number(), problems);
                case ImportConversion.Skipped<INPUT> ignored -> skipped++;
                case ImportConversion.Converted<INPUT>(var input) -> {
                    OUTPUT output;
                    try {
                        output = hooks.create().apply(input);
                    } catch (Exception e) {
                        throw hooks.createFailed().apply(row.number(), e);
                    }
                    hooks.created().accept(output);
                    created++;
                }
            }
        }
        return new BulkImportResult(created, skipped);
    }

    static <INPUT> BulkImportPreview<List<String>, ExcelMetadata> preview(
        Workbook workbook,
        @Nullable FormulaEvaluator evaluator,
        String sheetName,
        ExcelLayout layout,
        Hooks<INPUT, ?> hooks
    ) {
        var sheet = sheetOf(workbook, sheetName);
        var date1904 = isDate1904(workbook);
        var firstColumn = layout.firstColumn();
        var headerIndex = layout.headerRow() - 1;
        var firstDataIndex = layout.firstDataRow() - 1;

        var headers = List.<String>of();
        var previews = new ArrayList<ImportRecordPreview<List<String>>>();
        var created = 0;
        var skipped = 0;
        var rejected = 0;
        for (var raw : sheet) {
            var index = raw.getRowNum();
            if (index == headerIndex) {
                var header = new ExcelRow(raw, evaluator, date1904, firstColumn);
                headers = header.values(header.size());
                continue;
            }
            if (index < firstDataIndex) {
                continue;
            }
            var row = new ExcelRow(raw, evaluator, date1904, firstColumn);
            if (row.blank()) {
                continue;
            }
            switch (ImportConversion.attempt(() -> hooks.convert().apply(row))) {
                case ImportConversion.Unconvertible<INPUT>(var problems) -> {
                    rejected++;
                    previews.add(new ImportRecordPreview.Rejected<>(row.number(), row.values(row.size()), problems));
                }
                case ImportConversion.Skipped<INPUT>(var reason) -> {
                    skipped++;
                    previews.add(new ImportRecordPreview.Skipped<>(row.number(), row.values(row.size()), reason));
                }
                case ImportConversion.Converted<INPUT> ignored -> {
                    created++;
                    previews.add(new ImportRecordPreview.Created<>(row.number(), row.values(row.size())));
                }
            }
        }
        var metadata = headers.isEmpty() ? null : new ExcelMetadata(headers);
        return new BulkImportPreview<>(metadata, previews, created, skipped, rejected);
    }

    private static Sheet sheetOf(Workbook workbook, String sheetName) {
        var index = workbook.getSheetIndex(sheetName);
        if (index < 0) {
            throw new ExcelSheetMissingException(sheetName);
        }
        return workbook.getSheetAt(index);
    }

    private static boolean isDate1904(Workbook workbook) {
        return switch (workbook) {
            case Date1904Support support -> support.isDate1904();
            case HSSFWorkbook hssf -> hssf.getInternalWorkbook().isUsing1904DateWindowing();
            default -> false;
        };
    }
}
