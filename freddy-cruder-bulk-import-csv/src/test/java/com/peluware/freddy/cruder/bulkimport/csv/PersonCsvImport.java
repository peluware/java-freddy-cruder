package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportTemplate;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Imports people from a CSV file with the columns {@code nombre, edad, nacimiento, rol}. Which
 * records are skipped, before they are validated, and what {@link #createFailed} does are given by
 * the caller, so a test can say what it needs without a class of its own.
 */
class PersonCsvImport extends CsvBulkImportProvider<PersonInput, Long> {

    private final Predicate<CsvRow> skip;
    private final BiFunction<Integer, Exception, BulkImportRecordException> createFailed;

    private PersonCsvImport(
        PersonProvider people,
        Predicate<CsvRow> skip,
        BiFunction<Integer, Exception, BulkImportRecordException> createFailed
    ) {
        super(people);
        this.skip = skip;
        this.createFailed = createFailed;
    }

    static PersonCsvImport of(PersonProvider people) {
        return new PersonCsvImport(people, row -> false, BulkImportRecordException::new);
    }

    static PersonCsvImport skipping(PersonProvider people, Predicate<CsvRow> skip) {
        return new PersonCsvImport(people, skip, BulkImportRecordException::new);
    }

    static PersonCsvImport failingWith(PersonProvider people, BiFunction<Integer, Exception, BulkImportRecordException> createFailed) {
        return new PersonCsvImport(people, row -> false, createFailed);
    }

    @Override
    protected ImportConversion<PersonInput> convert(CsvRow row) {
        if (skip.test(row)) {
            return ImportConversion.skipped("Already exists");
        }
        var input = new PersonInput();
        var binder = new CsvRowBinder(row);
        binder.required(input::setName, 0, "nombre", CsvCell::textOrNull, "El nombre es obligatorio");
        binder.required(input::setAge, 1, "edad", CsvCell::integer, "La edad es obligatoria");
        binder.optional(input::setBirthDate, 2, "nacimiento", CsvCell::date);
        binder.required(input::setRole, 3, "rol", cell -> cell.enumeration(Role.class), "El rol es obligatorio");
        return binder.toConversion(input);
    }

    @Override
    protected BulkImportRecordException createFailed(int position, Exception failure) {
        return createFailed.apply(position, failure);
    }

    @Override
    public ImportTemplate template() {
        return new ImportTemplate("personas.csv", "text/csv", out -> {
        });
    }
}
