package com.peluware.freddy.cruder.export.csv;

import com.peluware.freddy.cruder.OwnedStreamProvider;
import com.peluware.freddy.cruder.export.ExportField;
import com.peluware.freddy.cruder.export.OwnedStreamExportProvider;
import org.apache.commons.csv.CSVFormat;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

/**
 * Exports every record an {@link OwnedStreamProvider} returns for a given owner as CSV, without
 * holding every matching record in memory at once. Owner-scoped counterpart of
 * {@link StreamingCsvExportProvider}.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type being exported
 */
public abstract class OwnedStreamingCsvExportProvider<OWNER_ID, OUTPUT> extends OwnedStreamExportProvider<OWNER_ID, OUTPUT> {

    /**
     * @param streamProvider the provider that streams every matching record of an owner
     */
    protected OwnedStreamingCsvExportProvider(OwnedStreamProvider<OWNER_ID, OUTPUT> streamProvider) {
        super(streamProvider);
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
    protected void writeRecords(OutputStream out, List<ExportField<OUTPUT>> fields, Stream<OUTPUT> records) {
        CsvWriting.write(out, charset(), format(), fields, records);
    }
}
