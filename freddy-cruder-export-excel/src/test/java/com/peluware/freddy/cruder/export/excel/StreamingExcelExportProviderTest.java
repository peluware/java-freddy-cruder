package com.peluware.freddy.cruder.export.excel;

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

class StreamingExcelExportProviderTest {

    private final PersonProvider people = new PersonProvider();
    private final PersonStreamingExcelExport export = new PersonStreamingExcelExport(people);

    private static List<List<String>> readRows(byte[] bytes) {
        var formatter = new DataFormatter();
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
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

        var out = new ByteArrayOutputStream();
        export.export(null, null, Sort.unsorted(), List.of()).writeTo(out);

        assertEquals(List.of(
            List.of("Name", "Age"),
            List.of("Ana", "30"),
            List.of("Luis", "25")
        ), readRows(out.toByteArray()));
    }

    @Test
    void rejectsFieldsTheExportDoesNotOffer() {
        var thrown = assertThrows(UnknownExportFieldsException.class,
            () -> export.export(null, null, Sort.unsorted(), List.of("email")));

        assertEquals(List.of("email"), thrown.unknown());
        assertEquals(List.of("name", "age"), thrown.known());
    }

    @Test
    void exportDoesNotReadAnything() {
        people.create(new PersonInput("Ana", 30));

        var resolved = export.export(null, null, Sort.unsorted(), List.of());
        people.create(new PersonInput("Luis", 25));

        var out = new ByteArrayOutputStream();
        resolved.writeTo(out);

        assertEquals(List.of(
            List.of("Name", "Age"),
            List.of("Ana", "30"),
            List.of("Luis", "25")
        ), readRows(out.toByteArray()));
    }
}
