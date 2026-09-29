package com.peluware.freddy.cruder.export;

import java.io.OutputStream;

/**
 * A single export already resolved against a search, query, sort and field selection —
 * returned by {@link ExportProvider#export} / {@link OwnedExportProvider#export}.
 *
 * <p>{@link #filename()} and {@link #mediaType()} are cheap and answered immediately, without
 * reading anything. {@link #writeTo} is where the actual read happens, and may be called once,
 * later, by whoever produces the response body — nothing about the split between the two depends
 * on being called from the same method invocation that created this {@code Export}.</p>
 */
public interface Export {

    /**
     * @return the name of the file, extension included
     */
    String filename();

    /**
     * @return the media type of the file
     */
    String mediaType();

    /**
     * Writes the file's content to {@code out}.
     *
     * @param out where to write; not closed
     */
    void writeTo(OutputStream out);
}
