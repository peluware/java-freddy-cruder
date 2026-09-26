package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.bulkimport.BulkImportPreview;
import com.peluware.freddy.cruder.bulkimport.BulkImportRecordException;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.ImportRecordPreview;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Reads a CSV file record by record, skipping blank records, and imports or previews each record
 * through the given hooks.
 */
final class CsvFileImport {

    /**
     * How the file is read.
     *
     * @param charset     the charset of the file
     * @param format      the format of the file
     * @param header      whether the first record holds the headers
     * @param firstColumn the first column of the headers and the data
     */
    record Reading(Charset charset, CSVFormat format, boolean header, int firstColumn) {
    }

    /**
     * What is done with each record.
     *
     * @param convert      converts a record into an input DTO, its problems, or nothing to do
     * @param create       creates a record
     * @param created      called after a record is created
     * @param createFailed builds the exception thrown when creating a record fails
     * @param <INPUT>      the input DTO type
     * @param <OUTPUT>     the output type returned when creating
     */
    record Hooks<INPUT, OUTPUT>(
        Function<CsvRow, ImportConversion<INPUT>> convert,
        Function<INPUT, OUTPUT> create,
        Consumer<OUTPUT> created,
        BiFunction<Integer, Exception, BulkImportRecordException> createFailed
    ) {
    }

    private CsvFileImport() {
    }

    static <INPUT, OUTPUT> BulkImportResult execute(InputStream in, Reading reading, Hooks<INPUT, OUTPUT> hooks) {
        try (var parser = openParser(in, reading)) {
            var created = 0;
            var skipped = 0;
            var positionOffset = reading.header() ? 1 : 0;
            for (var record : parser) {
                var row = new CsvRow(record, positionOffset, reading.firstColumn());
                if (row.blank()) {
                    continue;
                }
                switch (ImportConversion.attempt(() -> hooks.convert().apply(row))) {
                    case ImportConversion.Unconvertible<INPUT>(var problems) -> throw new BulkImportRecordException(row.number(), problems);
                    case ImportConversion.Skipped<INPUT> ignored -> skipped++;
                    case ImportConversion.Converted<INPUT>(var input) -> {
                        OUTPUT output;
                        try {
                            output = hooks.create().apply(input);
                        } catch (Exception e) {
                            throw hooks.createFailed().apply(row.number(), e);
                        }
                        hooks.created().accept(output);
                        created++;
                    }
                }
            }
            return new BulkImportResult(created, skipped);
        } catch (IOException e) {
            throw new UncheckedIOException("The file could not be closed", e);
        }
    }

    static <INPUT> BulkImportPreview<List<String>, CsvMetadata> preview(InputStream in, Reading reading, Hooks<INPUT, ?> hooks) {
        try (var parser = openParser(in, reading)) {
            var headers = reading.header() ? headersFrom(parser.getHeaderNames(), reading.firstColumn()) : List.<String>of();
            var positionOffset = reading.header() ? 1 : 0;

            var previews = new ArrayList<ImportRecordPreview<List<String>>>();
            var created = 0;
            var skipped = 0;
            var rejected = 0;
            for (var record : parser) {
                var row = new CsvRow(record, positionOffset, reading.firstColumn());
                if (row.blank()) {
                    continue;
                }
                switch (ImportConversion.attempt(() -> hooks.convert().apply(row))) {
                    case ImportConversion.Unconvertible<INPUT>(var problems) -> {
                        rejected++;
                        previews.add(new ImportRecordPreview.Rejected<>(row.number(), row.values(row.size()), problems));
                    }
                    case ImportConversion.Skipped<INPUT>(var reason) -> {
                        skipped++;
                        previews.add(new ImportRecordPreview.Skipped<>(row.number(), row.values(row.size()), reason));
                    }
                    case ImportConversion.Converted<INPUT> ignored -> {
                        created++;
                        previews.add(new ImportRecordPreview.Created<>(row.number(), row.values(row.size())));
                    }
                }
            }
            var metadata = headers.isEmpty() ? null : new CsvMetadata(headers);
            return new BulkImportPreview<>(metadata, previews, created, skipped, rejected);
        } catch (IOException e) {
            throw new UncheckedIOException("The file could not be closed", e);
        }
    }

    private static CSVParser openParser(InputStream in, Reading reading) {
        var reader = new InputStreamReader(in, reading.charset());
        try {
            var format = reading.header() ? reading.format().builder().setHeader().setSkipHeaderRecord(true).get() : reading.format();
            return format.parse(withoutByteOrderMark(reader));
        } catch (IOException e) {
            closeAfterFailure(reader, e);
            throw new UncheckedIOException("The file could not be read", e);
        } catch (RuntimeException e) {
            closeAfterFailure(reader, e);
            throw e;
        }
    }

    private static List<String> headersFrom(List<? extends @Nullable String> names, int firstColumn) {
        var headers = new ArrayList<String>();
        for (var index = firstColumn; index < names.size(); index++) {
            headers.add(Objects.requireNonNullElse(names.get(index), ""));
        }
        return headers;
    }

    private static Reader withoutByteOrderMark(Reader reader) throws IOException {
        var buffered = reader instanceof BufferedReader existing ? existing : new BufferedReader(reader);
        buffered.mark(1);
        if (buffered.read() != '﻿') {
            buffered.reset();
        }
        return buffered;
    }

    private static void closeAfterFailure(Reader reader, Exception failure) {
        try {
            reader.close();
        } catch (IOException closing) {
            failure.addSuppressed(closing);
        }
    }
}
