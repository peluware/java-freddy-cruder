package com.peluware.freddy.cruder.bulkimport;

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What a bulk import would do, without doing it; every record is checked.
 *
 * @param <PREVIEW>      the type of the data of each record
 * @param <PREVIEW_META> the type of the metadata of the origin
 * @param metadata       the metadata of the origin, or {@code null} if it has none
 * @param records        one entry per record, in the order of the origin
 * @param created        how many would be created
 * @param skipped        how many would be skipped; they do not prevent the import
 * @param rejected       how many have something that prevents the import
 */
public record BulkImportPreview<PREVIEW, PREVIEW_META>(
    @Nullable PREVIEW_META metadata,
    List<ImportRecordPreview<PREVIEW>> records,
    int created,
    int skipped,
    int rejected
) {
}
