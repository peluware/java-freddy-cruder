package com.peluware.freddy.cruder.export.csv;

import com.peluware.domain.Order;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.export.UnknownExportFieldsException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvExportProviderTest {

    private final PersonProvider people = new PersonProvider();
    private final PersonCsvExport export = new PersonCsvExport(people);

    private static String write(PersonCsvExport export, String search, String query, Sort sort, List<String> fields) {
        var out = new ByteArrayOutputStream();
        export.export(search, query, sort, fields).writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void describesTheFilenameAndMediaTypeUpfront() {
        var resolved = export.export(null, null, Sort.unsorted(), List.of());

        assertEquals("people.csv", resolved.filename());
        assertEquals("text/csv", resolved.mediaType());
    }

    @Test
    void exportsEveryFieldWhenNoneAreRequested() {
        people.create(new PersonInput("Ana", 30));
        people.create(new PersonInput("Luis", 25));

        var text = write(export, null, null, Sort.unsorted(), List.of());

        assertEquals("Name,Age\r\nAna,30\r\nLuis,25\r\n", text);
    }

    @Test
    void exportsOnlyTheRequestedFieldsInTheirDeclaredOrder() {
        people.create(new PersonInput("Ana", 30));

        var text = write(export, null, null, Sort.unsorted(), List.of("age"));

        assertEquals("Age\r\n30\r\n", text);
    }

    @Test
    void honorsTheQuerysSortWhenReadingFromTheProvider() {
        people.create(new PersonInput("Ana", 30));
        people.create(new PersonInput("Luis", 25));

        var text = write(export, null, null, Sort.by("age", Order.Direction.ASC), List.of());

        assertEquals("Name,Age\r\nLuis,25\r\nAna,30\r\n", text);
    }

    @Test
    void quotesValuesThatContainTheDelimiter() {
        people.create(new PersonInput("Doe, Ana", 30));

        var text = write(export, null, null, Sort.unsorted(), List.of());

        assertEquals("Name,Age\r\n\"Doe, Ana\",30\r\n", text);
    }

    @Test
    void rejectsFieldsTheExportDoesNotOffer() {
        var thrown = assertThrows(UnknownExportFieldsException.class,
            () -> export.export(null, null, Sort.unsorted(), List.of("email")));

        assertEquals(List.of("email"), thrown.unknown());
        assertEquals(List.of("name", "age"), thrown.known());
    }
}
