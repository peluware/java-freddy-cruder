package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.bulkimport.ImportProblem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvBulkImportProviderTest {

    private static final String HEADER = "nombre,edad,nacimiento,rol\n";

    private PersonProvider people;

    @BeforeEach
    void setUp() {
        people = new PersonProvider();
    }

    private static InputStream csv(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void createsEveryRecordWithItsTypedValues() {
        var result = PersonCsvImport.of(people).execute(csv(HEADER
            + "Ana,30,1995-03-05,ADMIN\n"
            + "Luis,25,,user\n"
            + "María,41,1980-12-01,USER\n"));

        assertEquals(new BulkImportResult(3, 0), result);
        var ana = people.entities().getFirst();
        assertEquals(30, ana.age);
        assertEquals(LocalDate.of(1995, 3, 5), ana.birthDate);
        assertEquals(Role.ADMIN, ana.role);
        var luis = people.entities().get(1);
        assertNull(luis.birthDate);
        assertEquals(Role.USER, luis.role);
    }

    @Test
    void skipsBlankRecords() {
        var result = PersonCsvImport.of(people).execute(csv(HEADER + "Ana,30,,ADMIN\n,,,\nLuis,25,,USER\n"));

        assertEquals(new BulkImportResult(2, 0), result);
    }

    @Test
    void stopsAtTheFirstRecordThatCannotBeConverted() {
        var exception = assertThrows(BulkImportRecordException.class,
            () -> PersonCsvImport.of(people).execute(csv(HEADER + "Ana,30,,ADMIN\n,25,,USER\nLuis,41,,USER\n")));

        assertEquals(3, exception.position());
        assertEquals("nombre", exception.problems().getFirst().field());
        assertEquals(1, people.entities().size());
    }

    @Test
    void reportsEveryProblemOfARecordAtOnce() {
        var exception = assertThrows(BulkImportRecordException.class,
            () -> PersonCsvImport.of(people).execute(csv(HEADER + ",abc,,otro\n")));

        assertEquals(List.of("nombre", "edad", "rol"), exception.problems().stream().map(ImportProblem::field).toList());
    }

    @Test
    void anOptionalValueThatIsInvalidIsStillAProblem() {
        var exception = assertThrows(BulkImportRecordException.class,
            () -> PersonCsvImport.of(people).execute(csv(HEADER + "Ana,30,not-a-date,ADMIN\n")));

        assertEquals("nacimiento", exception.problems().getFirst().field());
    }

    @Test
    void leavesAloneTheRecordsThatAreSkipped() {
        var importer = PersonCsvImport.skipping(people, row -> row.cell(0).text().equals("Ana"));

        var result = importer.execute(csv(HEADER + "Ana,30,,ADMIN\nLuis,25,,USER\n"));

        assertEquals(new BulkImportResult(1, 1), result);
    }

    @Test
    void aSkippedRecordIsNotValidated() {
        var importer = PersonCsvImport.skipping(people, row -> row.cell(0).text().equals("Ana"));

        var result = importer.execute(csv(HEADER + "Ana,abc,,otro\n"));

        assertEquals(new BulkImportResult(0, 1), result);
    }

    @Test
    void createFailedTranslatesAFailureOfTheCreation() {
        var importer = PersonCsvImport.failingWith(people, (position, failure) ->
            new BulkImportRecordException(position, List.of(ImportProblem.of("nombre", "Nombre rechazado")), failure));

        var exception = assertThrows(BulkImportRecordException.class, () -> importer.execute(csv(HEADER + "boom,30,,ADMIN\n")));

        assertEquals(2, exception.position());
        assertEquals("Nombre rechazado", exception.problems().getFirst().message());
    }

    @Test
    void previewCountsEachOutcomeAndCreatesNothing() {
        var importer = PersonCsvImport.skipping(people, row -> row.cell(0).text().equals("Luis"));

        var preview = importer.preview(csv(HEADER + "Ana,30,,ADMIN\n,25,,USER\nLuis,41,,USER\n"));

        assertEquals(1, preview.created());
        assertEquals(1, preview.skipped());
        assertEquals(1, preview.rejected());
        assertEquals(3, preview.records().size());
        var metadata = preview.metadata();
        assertNotNull(metadata);
        assertEquals(List.of("nombre", "edad", "nacimiento", "rol"), metadata.headers());
        assertTrue(people.entities().isEmpty());
    }

    @Test
    void ignoresAByteOrderMarkAtTheStartOfTheFile() {
        var preview = PersonCsvImport.of(people).preview(csv("﻿" + HEADER + "Ana,30,,ADMIN\n"));

        var metadata = preview.metadata();
        assertNotNull(metadata);
        assertEquals("nombre", metadata.headers().getFirst());
    }
}
