package com.peluware.freddy.cruder.export;

import java.util.List;

/**
 * Thrown when a requested field selection asks for a key an {@link ExportProvider} or
 * {@link OwnedExportProvider} doesn't offer.
 */
public class UnknownExportFieldsException extends RuntimeException {

    private final transient List<String> unknown;
    private final transient List<String> known;

    /**
     * @param unknown the requested keys nobody recognizes
     * @param known   the keys the provider actually offers
     */
    public UnknownExportFieldsException(List<String> unknown, List<String> known) {
        super("Unknown export fields: " + String.join(", ", unknown) + ". Known fields: " + String.join(", ", known));
        this.unknown = List.copyOf(unknown);
        this.known = List.copyOf(known);
    }

    /**
     * @return the requested keys nobody recognizes
     */
    public List<String> unknown() {
        return unknown;
    }

    /**
     * @return the keys the provider actually offers
     */
    public List<String> known() {
        return known;
    }
}
