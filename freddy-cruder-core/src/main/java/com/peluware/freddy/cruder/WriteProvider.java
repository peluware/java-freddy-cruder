package com.peluware.freddy.cruder;

/**
 * Composes all write operations for a resource: creation, update, and deletion.
 *
 * @param <ID>     the unique identifier type of the resource
 * @param <INPUT>  the input DTO type used to create or update resources
 * @param <OUTPUT> the output DTO type returned to the consumer
 * @see CreateProvider
 * @see UpdateProvider
 * @see DeleteProvider
 */
public interface WriteProvider<ID, INPUT, OUTPUT> extends
        CreateProvider<INPUT, OUTPUT>,
        UpdateProvider<ID, INPUT, OUTPUT>,
        DeleteProvider<ID> {
}
