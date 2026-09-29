package com.peluware.freddy.cruder.export.excel;

import com.peluware.freddy.cruder.OwnedStreamProvider;
import com.peluware.freddy.cruder.export.ExportField;
import com.peluware.freddy.cruder.export.OwnedStreamExportProvider;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.stream.Stream;

/**
 * Exports every record an {@link OwnedStreamProvider} returns for a given owner as an Excel
 * workbook, using {@link SXSSFWorkbook} to flush rows to a temporary file as they're written,
 * instead of holding the whole sheet in memory. Owner-scoped counterpart of
 * {@link StreamingExcelExportProvider}.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type being exported
 */
public abstract class OwnedStreamingExcelExportProvider<OWNER_ID, OUTPUT> extends OwnedStreamExportProvider<OWNER_ID, OUTPUT> {

    /**
     * @param streamProvider the provider that streams every matching record of an owner
     */
    protected OwnedStreamingExcelExportProvider(OwnedStreamProvider<OWNER_ID, OUTPUT> streamProvider) {
        super(streamProvider);
    }

    /**
     * @return how many rows {@link SXSSFWorkbook} keeps in memory before flushing older ones to a
     * temporary file; {@link SXSSFWorkbook#DEFAULT_WINDOW_SIZE} by default
     */
    protected int windowSize() {
        return SXSSFWorkbook.DEFAULT_WINDOW_SIZE;
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
     * <p>Rows fall out of {@code workbook}'s memory window as they're written (that's the point of
     * {@link SXSSFWorkbook}) — read {@code records} forward only, and don't revisit an earlier row.</p>
     *
     * @param workbook the workbook to fill; not written to the output or closed by this method
     * @param fields   the fields to include, in the order they should appear
     * @param records  every matching record, lazily; do not let it escape this call
     */
    protected void writeWorkbook(Workbook workbook, List<ExportField<OUTPUT>> fields, Stream<OUTPUT> records) {
        ExcelWriting.write(workbook, "Sheet1", fields, records);
    }

    @Override
    protected final void writeRecords(OutputStream out, List<ExportField<OUTPUT>> fields, Stream<OUTPUT> records) {
        try (var workbook = new SXSSFWorkbook(windowSize())) {
            writeWorkbook(workbook, fields, records);
            workbook.write(out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
