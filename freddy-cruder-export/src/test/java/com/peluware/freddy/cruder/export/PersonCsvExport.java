package com.peluware.freddy.cruder.export;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * A deliberately trivial writer — one line per field, comma-joined — just enough to prove
 * {@link ListExportProvider} hands {@link #writeRecords} the right records and the right field
 * selection. A real format lives in its own module, same as bulk import keeps Excel and CSV apart
 * from the core contract.
 */
class PersonCsvExport extends ListExportProvider<PersonOutput> {

    static final List<ExportField<PersonOutput>> FIELDS = List.of(
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
    protected String mediaType() {
        return "text/csv";
    }

    @Override
    protected List<ExportField<PersonOutput>> fields() {
        return FIELDS;
    }

    @Override
    protected void writeRecords(OutputStream out, List<ExportField<PersonOutput>> fields, List<PersonOutput> records) {
        /* PrintWriter#println would use the platform's line separator; a fixed "\n" keeps the test deterministic. */
        var writer = new PrintWriter(out, false, StandardCharsets.UTF_8);
        writer.write(String.join(",", fields.stream().map(ExportField::label).toList()) + "\n");
        for (var record : records) {
            writer.write(fields.stream().map(field -> String.valueOf(field.of(record))).reduce((a, b) -> a + "," + b).orElse("") + "\n");
        }
        writer.flush();
    }
}
