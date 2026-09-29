package com.peluware.freddy.cruder.export.excel;

import com.peluware.freddy.cruder.ListProvider;
import com.peluware.freddy.cruder.export.ExportField;
import com.peluware.freddy.cruder.export.ListExportProvider;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Exports every record a {@link ListProvider} returns as an Excel workbook, built in memory with
 * {@link XSSFWorkbook}. Prefer {@link StreamingExcelExportProvider} for a large export, so it
 * isn't held in memory in full before being written.
 *
 * @param <OUTPUT> the output DTO or projection type being exported
 */
public abstract class ExcelExportProvider<OUTPUT> extends ListExportProvider<OUTPUT> {

    /**
     * @param listProvider the provider that lists every matching record
     */
    protected ExcelExportProvider(ListProvider<OUTPUT> listProvider) {
        super(listProvider);
    }

    @Override
    protected String mediaType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    /**
     * Builds the workbook's content. Do whatever you want with {@code fields} and {@code records} —
     * sheet name, columns, styles, merges, a starting position other than {@code (0, 0)}; freddy-cruder
     * only guarantees the workbook is written to the output afterward. The default writes a single
     * unstyled sheet, "Sheet1", with a header row of field labels and one row per record — see
     * {@link ExcelWriting} for the pieces it's built from.
     *
     * @param workbook the workbook to fill; not written to the output or closed by this method
     * @param fields   the fields to include, in the order they should appear
     * @param records  every matching record
     */
    protected void writeWorkbook(Workbook workbook, List<ExportField<OUTPUT>> fields, List<OUTPUT> records) {
        ExcelWriting.write(workbook, "Sheet1", fields, records.stream());
    }

    @Override
    protected final void writeRecords(OutputStream out, List<ExportField<OUTPUT>> fields, List<OUTPUT> records) {
        try (var workbook = new XSSFWorkbook()) {
            writeWorkbook(workbook, fields, records);
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
