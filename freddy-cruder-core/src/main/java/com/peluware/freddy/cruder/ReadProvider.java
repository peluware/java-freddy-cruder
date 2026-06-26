package com.peluware.freddy.cruder;

/**
 * Composes all read operations for a resource: pagination, lookup, count, and existence check.
 *
 * @param <ID>     the unique identifier type of the resource
 * @param <OUTPUT> the output DTO type returned to the consumer
 * @see PageProvider
 * @see FindProvider
 * @see CountProvider
 * @see ExistsProvider
 */
public interface ReadProvider<ID, OUTPUT> extends
        PageProvider<OUTPUT>,
        FindProvider<ID, OUTPUT>,
        CountProvider,
        ExistsProvider<ID> {
}
