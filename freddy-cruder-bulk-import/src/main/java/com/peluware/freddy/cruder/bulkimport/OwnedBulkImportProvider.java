package com.peluware.freddy.cruder.bulkimport;

import com.peluware.freddy.cruder.OwnedCreateProvider;

import java.io.InputStream;
import java.util.Objects;

/**
 * Imports the records of a file under an owner by creating each one through an {@link OwnedCreateProvider}.
 *
 * @param <OWNER_ID>     the identifier type of the owning resource
 * @param <INPUT>        the input DTO type used to create the resource
 * @param <PREVIEW>      the type of the data of each record
 * @param <PREVIEW_META> the type of the metadata of the file
 * @param <OUTPUT>       the output DTO or projection type returned by the provider
 */
public abstract class OwnedBulkImportProvider<OWNER_ID, INPUT, PREVIEW, PREVIEW_META, OUTPUT> {

    /**
     * The provider that creates each record.
     */
    protected final OwnedCreateProvider<OWNER_ID, INPUT, OUTPUT> createProvider;

    /**
     * @param createProvider the provider that creates each record
     */
    protected OwnedBulkImportProvider(OwnedCreateProvider<OWNER_ID, INPUT, OUTPUT> createProvider) {
        this.createProvider = Objects.requireNonNull(createProvider, "Create provider must not be null");
    }

    /**
     * Creates every record of the file that is not skipped under {@code ownerId}, stopping at the
     * first one that fails.
     *
     * @param ownerId the identifier of the owning resource
     * @param in      the file to import; not closed
     * @return how many records were created and how many skipped
     * @throws BulkImportRecordException at the first record that fails
     */
    public abstract BulkImportResult execute(OWNER_ID ownerId, InputStream in);

    /**
     * Checks every record of the file without creating anything.
     *
     * @param ownerId the identifier of the owning resource
     * @param in      the file to check; not closed
     * @return what {@link #execute} would do with each record
     */
    public abstract BulkImportPreview<PREVIEW, PREVIEW_META> preview(OWNER_ID ownerId, InputStream in);

    /**
     * Builds the import template for an owner. It loads what the template needs when called, since
     * its writer may run later.
     *
     * @param ownerId the identifier of the owning resource
     * @return the template file
     */
    public abstract ImportTemplate template(OWNER_ID ownerId);

    /**
     * Called after each record is created.
     *
     * @param ownerId the identifier of the owning resource
     * @param output  what the provider returned
     */
    protected void created(OWNER_ID ownerId, OUTPUT output) {
    }

    /**
     * Builds the exception thrown when creating a record fails.
     *
     * @param position the position of the failing record in the file
     * @param failure  what was thrown
     * @return the exception to throw; by default one carrying the position and the failure as cause
     */
    protected BulkImportRecordException createFailed(int position, Exception failure) {
        return new BulkImportRecordException(position, failure);
    }
}
