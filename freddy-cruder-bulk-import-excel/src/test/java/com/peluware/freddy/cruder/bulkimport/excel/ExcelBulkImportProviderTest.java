package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.peluware.freddy.cruder.bulkimport.excel.PersonRows.SHEET;
import static com.peluware.freddy.cruder.bulkimport.excel.Workbooks.encrypted;
import static com.peluware.freddy.cruder.bulkimport.excel.Workbooks.person;
import static com.peluware.freddy.cruder.bulkimport.excel.Workbooks.workbook;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ExcelBulkImportProviderTest extends AbstractExcelBulkImportProviderTest {

    @Override
    protected AbstractExcelBulkImportProvider<PersonInput, Long> importer() {
        return ClassicPersonImport.of(people);
    }

    @Override
    protected AbstractExcelBulkImportProvider<PersonInput, Long> importerSkipping(Predicate<ExcelRow> skip) {
        return ClassicPersonImport.skipping(people, skip);
    }

    @Override
    protected AbstractExcelBulkImportProvider<PersonInput, Long> importerFailingWith(BiFunction<Integer, Exception, BulkImportRecordException> createFailed) {
        return ClassicPersonImport.failingWith(people, createFailed);
    }

    @Test
    void evaluatesAFormulaThatWasNeverCalculated() {
        var result = importer().execute(workbook(SHEET,
            new @Nullable Object[]{"Ana", new Workbooks.Formula("10+20"), null, "ADMIN"}));

        assertEquals(new BulkImportResult(1, 0), result);
        assertEquals(30, people.entities().getFirst().age);
    }

    @Test
    void opensAFileThatNeedsAPasswordWhenHowItIsOpenedIsOverridden() {
        var file = encrypted("secreto", workbook(SHEET, person("Ana", 30, null, "ADMIN")));

        var result = new PasswordPersonImport(people, "secreto").execute(file);

        assertEquals(new BulkImportResult(1, 0), result);
    }
}
