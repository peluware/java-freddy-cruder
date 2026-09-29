package com.peluware.freddy.cruder.export;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Owner-scoped counterpart of {@link AbstractExportProvider}. Shared by
 * {@link OwnedListExportProvider} and {@link OwnedStreamExportProvider}, which differ only in how
 * the {@link Export} they build actually reads its records.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type being exported
 */
public abstract class AbstractOwnedExportProvider<OWNER_ID, OUTPUT> implements OwnedExportProvider<OWNER_ID> {

    /**
     * @return the name of the file, extension included
     */
    protected abstract String filename();

    /**
     * @return the media type of the file
     */
    protected abstract String mediaType();

    /**
     * @return every field this export can include; a request may ask for a subset
     */
    protected abstract List<ExportField<OUTPUT>> fields();

    /**
     * Builds the {@link Export} to return from {@link #export}: {@link #filename()} and
     * {@link #mediaType()} resolved right away, {@code writer} deferred to {@link Export#writeTo}.
     *
     * @param writer writes the file's content to the given {@link OutputStream} when called
     */
    protected final Export newExport(Consumer<OutputStream> writer) {
        return new AbstractExport(filename(), mediaType()) {
            @Override
            public void writeTo(OutputStream out) {
                writer.accept(out);
            }
        };
    }
}
