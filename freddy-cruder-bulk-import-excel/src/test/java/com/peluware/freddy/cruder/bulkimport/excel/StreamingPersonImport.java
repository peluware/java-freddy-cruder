package com.peluware.freddy.cruder.bulkimport.excel;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Imports people with {@link StreamingExcelBulkImportProvider}. Which rows are skipped, before they
 * are validated, and what {@link #createFailed} does are given by the caller, so a test can say what
 * it needs without a class of its own.
 */
class StreamingPersonImport extends StreamingExcelBulkImportProvider<PersonInput, Long> {

    private final Predicate<ExcelRow> skip;
    private final BiFunction<Integer, Exception, BulkImportRecordException> createFailed;

    private StreamingPersonImport(
        PersonProvider people,
        Predicate<ExcelRow> skip,
        BiFunction<Integer, Exception, BulkImportRecordException> createFailed
    ) {
        super(people);
        this.skip = skip;
        this.createFailed = createFailed;
    }

    static StreamingPersonImport of(PersonProvider people) {
        return new StreamingPersonImport(people, row -> false, BulkImportRecordException::new);
    }

    static StreamingPersonImport skipping(PersonProvider people, Predicate<ExcelRow> skip) {
        return new StreamingPersonImport(people, skip, BulkImportRecordException::new);
    }

    static StreamingPersonImport failingWith(PersonProvider people, BiFunction<Integer, Exception, BulkImportRecordException> createFailed) {
        return new StreamingPersonImport(people, row -> false, createFailed);
    }

    @Override
    protected String sheetName() {
        return PersonRows.SHEET;
    }

    @Override
    protected ImportConversion<PersonInput> convert(ExcelRow row) {
        return skip.test(row) ? ImportConversion.skipped("Already exists") : PersonRows.convert(row);
    }

    @Override
    protected BulkImportRecordException createFailed(int position, Exception failure) {
        return createFailed.apply(position, failure);
    }

    @Override
    public ImportTemplate template() {
        return new ImportTemplate("personas.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out -> {
        });
    }
}
