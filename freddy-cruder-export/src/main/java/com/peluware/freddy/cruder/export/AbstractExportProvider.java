package com.peluware.freddy.cruder.export;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * An {@link ExportProvider} whose name, media type and field selection come from
 * {@link #filename()}, {@link #mediaType()} and {@link #fields()}. Shared by
 * {@link ListExportProvider} and {@link StreamExportProvider}, which differ only in how
 * the {@link Export} they build actually reads its records.
 *
 * @param <OUTPUT> the output DTO or projection type being exported
 */
public abstract class AbstractExportProvider<OUTPUT> implements ExportProvider {

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
