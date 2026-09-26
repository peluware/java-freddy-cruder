package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.ImportProblem;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import static com.peluware.freddy.cruder.bulkimport.excel.PersonRows.SHEET;
import static com.peluware.freddy.cruder.bulkimport.excel.Workbooks.workbook;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StreamingExcelBulkImportProviderTest extends AbstractExcelBulkImportProviderTest {

    @Override
    protected AbstractExcelBulkImportProvider<PersonInput, Long> importer() {
        return StreamingPersonImport.of(people);
    }

    @Override
    protected AbstractExcelBulkImportProvider<PersonInput, Long> importerSkipping(Predicate<ExcelRow> skip) {
        return StreamingPersonImport.skipping(people, skip);
    }

    @Override
    protected AbstractExcelBulkImportProvider<PersonInput, Long> importerFailingWith(BiFunction<Integer, Exception, BulkImportRecordException> createFailed) {
        return StreamingPersonImport.failingWith(people, createFailed);
    }

    @Test
    void readsAFormulaThatWasNeverCalculatedAsEmpty() {
        var exception = assertThrows(BulkImportRecordException.class, () -> importer().execute(workbook(SHEET,
            new @Nullable Object[]{"Ana", new Workbooks.Formula("10+20"), null, "ADMIN"})));

        assertEquals(List.of("edad"), exception.problems().stream().map(ImportProblem::field).toList());
    }
}
