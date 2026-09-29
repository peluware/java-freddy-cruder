package com.peluware.freddy.cruder.export.csv;

import com.peluware.freddy.cruder.OwnedListProvider;
import com.peluware.freddy.cruder.export.ExportField;
import com.peluware.freddy.cruder.export.OwnedListExportProvider;
import org.apache.commons.csv.CSVFormat;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports every record an {@link OwnedListProvider} returns for a given owner as CSV. Owner-scoped
 * counterpart of {@link CsvExportProvider}.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type being exported
 */
public abstract class OwnedCsvExportProvider<OWNER_ID, OUTPUT> extends OwnedListExportProvider<OWNER_ID, OUTPUT> {

    /**
     * @param listProvider the provider that lists every matching record of an owner
     */
    protected OwnedCsvExportProvider(OwnedListProvider<OWNER_ID, OUTPUT> listProvider) {
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
