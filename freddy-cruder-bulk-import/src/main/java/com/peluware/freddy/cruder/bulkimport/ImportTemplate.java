package com.peluware.freddy.cruder.bulkimport;

import java.io.OutputStream;
import java.util.Objects;

/**
 * A downloadable import template: file metadata plus a writer of its content.
 *
 * @param name      the name of the file, extension included, such as {@code accounts.xlsx}
 * @param mediaType the media type of the content, such as {@code text/csv}
 * @param writer    writes the content
 */
public record ImportTemplate(String name, String mediaType, Writer writer) {

    public ImportTemplate {
        Objects.requireNonNull(name, "Name must not be null");
        Objects.requireNonNull(mediaType, "Media type must not be null");
        Objects.requireNonNull(writer, "Writer must not be null");
        if (name.isBlank() || mediaType.isBlank()) {
            throw new IllegalArgumentException("Name and media type must not be blank");
        }
    }

    /**
     * Writes the content.
     *
     * @param out where to write it; it is not closed
     */
    public void writeTo(OutputStream out) {
        writer.writeTo(out);
    }

    /**
     * Writes the content of a template.
     */
    @FunctionalInterface
    public interface Writer {

        /**
         * @param out where to write the content; it is not closed
         */
        void writeTo(OutputStream out);
    }
}
