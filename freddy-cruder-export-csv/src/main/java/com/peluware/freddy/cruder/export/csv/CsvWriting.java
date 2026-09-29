package com.peluware.freddy.cruder.export.csv;

import com.peluware.freddy.cruder.export.ExportField;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.stream.Stream;

/**
 * Writes records as CSV, shared by the list- and stream-backed CSV export providers.
 */
final class CsvWriting {

    private CsvWriting() {
        throw new UnsupportedOperationException("Utility class");
    }

    static <OUTPUT> void write(OutputStream out, Charset charset, CSVFormat format, List<ExportField<OUTPUT>> fields, Stream<OUTPUT> records) {
        var headeredFormat = format.builder()
            .setHeader(fields.stream().map(ExportField::label).toArray(String[]::new))
            .get();
        try (var printer = new CSVPrinter(new OutputStreamWriter(out, charset), headeredFormat)) {
            records.forEach(record -> printRecord(printer, fields, record));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static <OUTPUT> void printRecord(CSVPrinter printer, List<ExportField<OUTPUT>> fields, OUTPUT record) {
        try {
            printer.printRecord(fields.stream().map(field -> field.of(record)).toArray());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
