package com.peluware.freddy.cruder.export.excel;

import com.peluware.freddy.cruder.export.ExportField;

import java.util.List;

class PersonStreamingExcelExport extends StreamingExcelExportProvider<PersonOutput> {

    private static final List<ExportField<PersonOutput>> FIELDS = List.of(
        new ExportField<>("name", "Name", PersonOutput::name),
        new ExportField<>("age", "Age", PersonOutput::age)
    );

    PersonStreamingExcelExport(PersonProvider provider) {
        super(provider);
    }

    @Override
    protected String filename() {
        return "people.xlsx";
    }

    @Override
    protected List<ExportField<PersonOutput>> fields() {
        return FIELDS;
    }
}
