package com.peluware.freddy.cruder.export.excel;

import com.peluware.domain.Order;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.export.UnknownExportFieldsException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelExportProviderTest {

    private final PersonProvider people = new PersonProvider();
    private final PersonExcelExport export = new PersonExcelExport(people);

    private static List<List<String>> readRows(PersonExcelExport export, Sort sort, List<String> fields) {
        var out = new ByteArrayOutputStream();
        export.export(null, null, sort, fields).writeTo(out);
        var formatter = new DataFormatter();
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            var rows = new java.util.ArrayList<List<String>>();
            for (var row : sheet) {
                var cells = new java.util.ArrayList<String>();
                row.forEach(cell -> cells.add(formatter.formatCellValue(cell)));
                rows.add(cells);
            }
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void describesTheFilenameAndMediaTypeUpfront() {
        var resolved = export.export(null, null, Sort.unsorted(), List.of());

        assertEquals("people.xlsx", resolved.filename());
        assertEquals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", resolved.mediaType());
    }

    @Test
    void exportsEveryFieldWhenNoneAreRequested() {
        people.create(new PersonInput("Ana", 30));
        people.create(new PersonInput("Luis", 25));

        var rows = readRows(export, Sort.unsorted(), List.of());

        assertEquals(List.of(
            List.of("Name", "Age"),
            List.of("Ana", "30"),
            List.of("Luis", "25")
        ), rows);
    }

    @Test
    void exportsOnlyTheRequestedFieldsInTheirDeclaredOrder() {
        people.create(new PersonInput("Ana", 30));

        var rows = readRows(export, Sort.unsorted(), List.of("age"));

        assertEquals(List.of(List.of("Age"), List.of("30")), rows);
    }

    @Test
    void honorsTheQuerysSortWhenReadingFromTheProvider() {
        people.create(new PersonInput("Ana", 30));
        people.create(new PersonInput("Luis", 25));

        var rows = readRows(export, Sort.by("age", Order.Direction.ASC), List.of());

        assertEquals(List.of(
            List.of("Name", "Age"),
            List.of("Luis", "25"),
            List.of("Ana", "30")
        ), rows);
    }

    @Test
    void rejectsFieldsTheExportDoesNotOffer() {
        var thrown = assertThrows(UnknownExportFieldsException.class,
            () -> export.export(null, null, Sort.unsorted(), List.of("email")));

        assertEquals(List.of("email"), thrown.unknown());
        assertEquals(List.of("name", "age"), thrown.known());
    }

    @Test
    void honorsStartRowStartColumnAndCellStyler() {
        people.create(new PersonInput("Ana", 30));
        var export = new PersonExcelExportCustomized(people);

        var out = new ByteArrayOutputStream();
        export.export(null, null, Sort.unsorted(), List.of()).writeTo(out);

        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(out.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);

            assertEquals(null, sheet.getRow(0));

            var headerRow = sheet.getRow(1);
            var nameHeaderCell = headerRow.getCell(1);
            assertEquals("Name", nameHeaderCell.getStringCellValue());
            assertEquals(true, workbook.getFontAt(nameHeaderCell.getCellStyle().getFontIndex()).getBold());

            var dataRow = sheet.getRow(2);
            assertEquals("Ana", dataRow.getCell(1).getStringCellValue());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
