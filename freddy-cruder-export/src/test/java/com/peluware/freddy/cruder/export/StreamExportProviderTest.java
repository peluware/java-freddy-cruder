package com.peluware.freddy.cruder.export;

import com.peluware.domain.Order;
import com.peluware.domain.Sort;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamExportProviderTest {

    private final PersonProvider people = new PersonProvider();
    private final PersonStreamCsvExport export = new PersonStreamCsvExport(people);

    private static String write(PersonStreamCsvExport export, String search, String query, Sort sort, List<String> fields) {
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

        assertEquals("Name,Age\nAna,30\nLuis,25\n", text);
    }

    @Test
    void exportsOnlyTheRequestedFieldsInTheirDeclaredOrder() {
        people.create(new PersonInput("Ana", 30));

        var text = write(export, null, null, Sort.unsorted(), List.of("age"));

        assertEquals("Age\n30\n", text);
    }

    @Test
    void honorsTheQuerysSortWhenReadingFromTheProvider() {
        people.create(new PersonInput("Ana", 30));
        people.create(new PersonInput("Luis", 25));

        var text = write(export, null, null, Sort.by("age", Order.Direction.ASC), List.of());

        assertEquals("Name,Age\nLuis,25\nAna,30\n", text);
    }

    @Test
    void rejectsFieldsTheExportDoesNotOffer() {
        var thrown = assertThrows(UnknownExportFieldsException.class,
            () -> export.export(null, null, Sort.unsorted(), List.of("email")));

        assertEquals(List.of("email"), thrown.unknown());
        assertEquals(List.of("name", "age"), thrown.known());
    }

    /**
     * {@code export} never touches the provider — this pins that down, so nobody accidentally
     * makes it read anything before {@code writeTo} is actually called.
     */
    @Test
    void exportDoesNotReadAnything() {
        people.create(new PersonInput("Ana", 30));

        var resolved = export.export(null, null, Sort.unsorted(), List.of());
        people.create(new PersonInput("Luis", 25)); // created after export(), before writeTo()

        var out = new ByteArrayOutputStream();
        resolved.writeTo(out);

        assertEquals("Name,Age\nAna,30\nLuis,25\n", out.toString(StandardCharsets.UTF_8));
    }
}
