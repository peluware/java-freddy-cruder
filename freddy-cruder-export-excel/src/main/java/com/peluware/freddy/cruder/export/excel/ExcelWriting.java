package com.peluware.freddy.cruder.export.excel;

import com.peluware.freddy.cruder.export.ExportField;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Plain, unstyled table writer: one header row of field labels, one row per record, at a
 * configurable position, styled through a {@link CellStyler} the caller supplies. Backs the
 * default {@code writeWorkbook} of the Excel export providers — not invoked at all once that
 * method is overridden.
 */
final class ExcelWriting {

    private ExcelWriting() {
        throw new UnsupportedOperationException("Utility class");
    }

    static <OUTPUT> void write(Workbook workbook, String sheetName, List<ExportField<OUTPUT>> fields, Stream<OUTPUT> records) {
        write(workbook, sheetName, 0, 0, fields, records, CellStyler.NONE);
    }

    static <OUTPUT> void write(Workbook workbook, String sheetName, int startRow, int startColumn, List<ExportField<OUTPUT>> fields, Stream<OUTPUT> records, CellStyler styler) {
        var sheet = workbook.createSheet(sheetName);
        writeHeader(sheet, startRow, startColumn, fields, styler);
        var rowIndex = new AtomicInteger(startRow + 1);
        records.forEach(record -> writeRow(sheet, rowIndex.getAndIncrement(), startColumn, fields, record, styler));
    }

    private static <OUTPUT> void writeHeader(Sheet sheet, int rowIndex, int startColumn, List<ExportField<OUTPUT>> fields, CellStyler styler) {
        var header = sheet.createRow(rowIndex);
        for (var i = 0; i < fields.size(); i++) {
            var cell = header.createCell(startColumn + i);
            cell.setCellValue(fields.get(i).label());
            styler.style(cell, rowIndex, i, true);
        }
    }

    private static <OUTPUT> void writeRow(Sheet sheet, int rowIndex, int startColumn, List<ExportField<OUTPUT>> fields, OUTPUT record, CellStyler styler) {
        var row = sheet.createRow(rowIndex);
        for (var i = 0; i < fields.size(); i++) {
            var cell = row.createCell(startColumn + i);
            setCellValue(cell, fields.get(i).of(record));
            styler.style(cell, rowIndex, i, false);
        }
    }

    private static void setCellValue(Cell cell, @Nullable Object value) {
        switch (value) {
            case null -> cell.setBlank();
            case String string -> cell.setCellValue(string);
            case Number number -> cell.setCellValue(number.doubleValue());
            case Boolean bool -> cell.setCellValue(bool);
            case LocalDate date -> cell.setCellValue(date);
            case LocalDateTime date -> cell.setCellValue(date);
            case Calendar calendar -> cell.setCellValue(calendar);
            case Date date -> cell.setCellValue(date);
            default -> cell.setCellValue(value.toString());
        }
    }
}
