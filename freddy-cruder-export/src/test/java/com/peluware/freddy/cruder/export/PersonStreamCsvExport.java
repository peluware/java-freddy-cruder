package com.peluware.freddy.cruder.export;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * Same trivial CSV shape as {@link PersonCsvExport}, but over {@link StreamExportProvider} —
 * proves {@link #writeRecords} receives a live {@link Stream}, opened and closed by {@link #write}
 * around it, through {@link PersonProvider}'s {@code stream} (the default fallback from
 * {@code EntityCrudProvider}, still correct, just not a real cursor).
 */
class PersonStreamCsvExport extends StreamExportProvider<PersonOutput> {

    static final List<ExportField<PersonOutput>> FIELDS = List.of(
        new ExportField<>("name", "Name", PersonOutput::name),
        new ExportField<>("age", "Age", PersonOutput::age)
    );

    PersonStreamCsvExport(PersonProvider provider) {
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
    protected void writeRecords(OutputStream out, List<ExportField<PersonOutput>> fields, Stream<PersonOutput> records) {
        var writer = new PrintWriter(out, false, StandardCharsets.UTF_8);
        writer.write(String.join(",", fields.stream().map(ExportField::label).toList()) + "\n");
        records.forEach(record ->
            writer.write(fields.stream().map(field -> String.valueOf(field.of(record))).reduce((a, b) -> a + "," + b).orElse("") + "\n"));
        writer.flush();
    }
}
