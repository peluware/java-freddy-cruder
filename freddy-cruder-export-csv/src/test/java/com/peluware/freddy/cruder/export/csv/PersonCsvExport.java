package com.peluware.freddy.cruder.export.csv;

import com.peluware.freddy.cruder.export.ExportField;

import java.util.List;

class PersonCsvExport extends CsvExportProvider<PersonOutput> {

    private static final List<ExportField<PersonOutput>> FIELDS = List.of(
        new ExportField<>("name", "Name", PersonOutput::name),
        new ExportField<>("age", "Age", PersonOutput::age)
    );

    PersonCsvExport(PersonProvider provider) {
        super(provider);
    }

    @Override
    protected String filename() {
        return "people.csv";
    }

    @Override
    protected List<ExportField<PersonOutput>> fields() {
        return FIELDS;
    }
}
