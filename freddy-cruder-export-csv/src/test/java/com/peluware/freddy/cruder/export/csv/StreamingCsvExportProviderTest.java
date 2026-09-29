package com.peluware.freddy.cruder.export.csv;

import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.export.UnknownExportFieldsException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamingCsvExportProviderTest {

    private final PersonProvider people = new PersonProvider();
    private final PersonStreamingCsvExport export = new PersonStreamingCsvExport(people);

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

        var out = new ByteArrayOutputStream();
        export.export(null, null, Sort.unsorted(), List.of()).writeTo(out);

        assertEquals("Name,Age\r\nAna,30\r\nLuis,25\r\n", out.toString(StandardCharsets.UTF_8));
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

        assertEquals("Name,Age\r\nAna,30\r\nLuis,25\r\n", out.toString(StandardCharsets.UTF_8));
    }
}
