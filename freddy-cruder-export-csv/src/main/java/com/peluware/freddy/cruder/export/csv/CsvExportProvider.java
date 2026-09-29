package com.peluware.freddy.cruder.export.csv;

import com.peluware.freddy.cruder.ListProvider;
import com.peluware.freddy.cruder.export.ExportField;
import com.peluware.freddy.cruder.export.ListExportProvider;
import org.apache.commons.csv.CSVFormat;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports every record a {@link ListProvider} returns as CSV.
 *
 * @param <OUTPUT> the output DTO or projection type being exported
 */
public abstract class CsvExportProvider<OUTPUT> extends ListExportProvider<OUTPUT> {

    /**
     * @param listProvider the provider that lists every matching record
     */
    protected CsvExportProvider(ListProvider<OUTPUT> listProvider) {
        super(listProvider);
    }

    /**
     * @return the charset the file is written in; UTF-8 by default
     */
    protected Charset charset() {
        return StandardCharsets.UTF_8;
    }

    /**
     * @return the format of the file; {@link CSVFormat#DEFAULT} by default
     */
    protected CSVFormat format() {
        return CSVFormat.DEFAULT;
    }

    @Override
    protected String mediaType() {
        return "text/csv";
    }

    @Override
    protected void writeRecords(OutputStream out, List<ExportField<OUTPUT>> fields, List<OUTPUT> records) {
        CsvWriting.write(out, charset(), format(), fields, records.stream());
    }
}
