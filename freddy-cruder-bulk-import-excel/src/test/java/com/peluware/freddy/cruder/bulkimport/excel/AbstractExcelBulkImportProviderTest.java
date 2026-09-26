package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.bulkimport.ImportProblem;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.peluware.freddy.cruder.bulkimport.excel.PersonRows.SHEET;
import static com.peluware.freddy.cruder.bulkimport.excel.Workbooks.person;
import static com.peluware.freddy.cruder.bulkimport.excel.Workbooks.workbook;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every way of reading an Excel file has to do the same. Each subclass says which provider to
 * read with, and adds what only its way of reading does.
 */
abstract class AbstractExcelBulkImportProviderTest {

    protected PersonProvider people;

    @BeforeEach
    void setUp() {
        people = new PersonProvider();
    }

    protected abstract AbstractExcelBulkImportProvider<PersonInput, Long> importer();

    protected abstract AbstractExcelBulkImportProvider<PersonInput, Long> importerSkipping(Predicate<ExcelRow> skip);

    protected abstract AbstractExcelBulkImportProvider<PersonInput, Long> importerFailingWith(BiFunction<Integer, Exception, BulkImportRecordException> createFailed);

    @Test
    void createsEveryRowWithItsTypedValues() {

        var result = importer().execute(workbook(SHEET,
            person("Ana", 30, LocalDate.of(1995, 3, 5), "ADMIN"),
            person("Luis", 25, null, "user"),
            person("María", 41, LocalDate.of(1980, 12, 1), "USER"))
        );

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
    void skipsBlankRows() {

        var result = importer().execute(workbook(SHEET,
            person("Ana", 30, null, "ADMIN"),
            person(null, null, null, null),
            person("Luis", 25, null, "USER"))
        );

        assertEquals(new BulkImportResult(2, 0), result);
    }

    @Test
    void readsAnySizeOfSheet() {
        var rows = new @Nullable Object[5000][];
        for (var index = 0; index < rows.length; index++) {
            rows[index] = person("Persona " + index, 30, null, "USER");
        }

        var result = importer().execute(workbook(SHEET, rows));

        assertEquals(new BulkImportResult(5000, 0), result);
    }

    @Test
    void stopsAtTheFirstRowThatCannotBeConverted() {

        var exception = assertThrows(BulkImportRecordException.class, () -> importer().execute(workbook(SHEET,
            person("Ana", 30, null, "ADMIN"),
            person(null, 25, null, "USER"),
            person("Luis", 41, null, "USER")))
        );

        assertEquals(3, exception.position());
        assertEquals("nombre", exception.problems().getFirst().field());
        assertEquals(1, people.entities().size());
    }

    @Test
    void reportsEveryProblemOfARowAtOnce() {
        var exception = assertThrows(BulkImportRecordException.class, () -> importer().execute(workbook(SHEET,
            new @Nullable Object[]{null, "abc", null, "otro"})));

        assertEquals(List.of("nombre", "edad", "rol"), exception.problems().stream().map(ImportProblem::field).toList());
    }

    @Test
    void leavesAloneTheRowsThatAreSkipped() {

        var importer = importerSkipping(row -> row.cell(0).text().equals("Ana"));

        var result = importer.execute(workbook(SHEET,
            person("Ana", 30, null, "ADMIN"),
            person("Luis", 25, null, "USER"))
        );

        assertEquals(new BulkImportResult(1, 1), result);
    }

    @Test
    void aSkippedRowIsNotValidated() {
        var importer = importerSkipping(row -> row.cell(0).text().equals("Ana"));

        var result = importer.execute(workbook(SHEET, person("Ana", null, null, "otro")));

        assertEquals(new BulkImportResult(0, 1), result);
    }

    @Test
    void createFailedTranslatesAFailureOfTheCreation() {
        var importer = importerFailingWith((position, failure) ->
            new BulkImportRecordException(position, List.of(ImportProblem.of("nombre", "Nombre rechazado")), failure));

        var exception = assertThrows(BulkImportRecordException.class, () -> importer.execute(workbook(SHEET, person("boom", 30, null, "ADMIN"))));

        assertEquals(2, exception.position());
        assertEquals("Nombre rechazado", exception.problems().getFirst().message());
    }

    @Test
    void previewCountsEachOutcomeAndCreatesNothing() {
        var importer = importerSkipping(row -> row.cell(0).text().equals("Luis"));

        var preview = importer.preview(workbook(SHEET,
            person("Ana", 30, null, "ADMIN"),
            person(null, 25, null, "USER"),
            person("Luis", 41, null, "USER"))
        );

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
    void failsWhenTheSheetDoesNotExist() {
        assertThrows(ExcelSheetMissingException.class, () -> importer().execute(workbook("Otra", person("Ana", 30, null, "ADMIN"))));
    }
}
