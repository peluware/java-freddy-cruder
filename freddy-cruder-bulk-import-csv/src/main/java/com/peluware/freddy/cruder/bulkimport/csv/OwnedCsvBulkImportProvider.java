package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.OwnedCreateProvider;
import com.peluware.freddy.cruder.bulkimport.BulkImportPreview;
import com.peluware.freddy.cruder.bulkimport.BulkImportResult;
import com.peluware.freddy.cruder.bulkimport.ImportConversion;
import com.peluware.freddy.cruder.bulkimport.OwnedBulkImportProvider;
import org.apache.commons.csv.CSVFormat;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * An {@link OwnedBulkImportProvider} that reads a CSV file record by record, skipping blank records.
 * A byte order mark at the start is ignored.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <INPUT>    the input DTO type used to create the resource
 * @param <OUTPUT>   the output DTO or projection type returned by the provider
 */
public abstract class OwnedCsvBulkImportProvider<OWNER_ID, INPUT, OUTPUT> extends OwnedBulkImportProvider<OWNER_ID, INPUT, List<String>, CsvMetadata, OUTPUT> {

    /**
     * @param createProvider the provider that creates each record
     */
    protected OwnedCsvBulkImportProvider(OwnedCreateProvider<OWNER_ID, INPUT, OUTPUT> createProvider) {
        super(createProvider);
    }

    /**
     * @return the charset of the file; UTF-8 by default
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

    /**
     * @return whether the first record holds the headers; {@code true} by default
     */
    protected boolean header() {
        return true;
    }

    /**
     * @return the first column of the headers and the data; {@code 0} by default
     */
    protected int firstColumn() {
        return 0;
    }

    /**
     * Converts a record into its input DTO, or the problems that prevent it.
     *
     * @param ownerId the identifier of the owning resource
     * @param row     the record
     * @return the conversion of the record
     */
    protected abstract ImportConversion<INPUT> convert(OWNER_ID ownerId, CsvRow row);

    @Override
    public BulkImportResult execute(OWNER_ID ownerId, InputStream in) {
        return CsvFileImport.execute(in, reading(), hooks(ownerId));
    }

    @Override
    public BulkImportPreview<List<String>, CsvMetadata> preview(OWNER_ID ownerId, InputStream in) {
        return CsvFileImport.preview(in, reading(), hooks(ownerId));
    }

    private CsvFileImport.Reading reading() {
        return new CsvFileImport.Reading(charset(), format(), header(), firstColumn());
    }

    private CsvFileImport.Hooks<INPUT, OUTPUT> hooks(OWNER_ID ownerId) {
        return new CsvFileImport.Hooks<>(
            row -> convert(ownerId, row),
            input -> createProvider.create(ownerId, input),
            output -> created(ownerId, output),
            this::createFailed
        );
    }
}
