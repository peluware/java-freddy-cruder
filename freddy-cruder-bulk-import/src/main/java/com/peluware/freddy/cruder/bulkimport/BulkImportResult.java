package com.peluware.freddy.cruder.bulkimport;

/**
 * The outcome of a bulk import.
 *
 * @param created how many records were created
 * @param skipped how many were left alone
 */
public record BulkImportResult(int created, int skipped) {
}
