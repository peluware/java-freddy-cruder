package com.peluware.freddy.cruder;

/**
 * Generic CRUD provider designed to decouple application logic from persistence details.
 *
 * <p>
 * Implementations define how entities are created, retrieved, updated, deleted, and listed,
 * without imposing constraints on the underlying framework or technology.
 * </p>
 *
 * <p>
 * This contract supports extensibility through {@link CrudOptions}, allowing additional
 * metadata to be passed to modify the behavior of each operation (e.g. relation loading,
 * partial execution, audit flags, soft-delete, optimization hints, etc.).
 * </p>
 *
 * @param <ID>     the unique identifier type of the resource
 * @param <INPUT>  the input DTO type used to create or update resources
 * @param <OUTPUT> the output DTO type returned to the consumer
 * @see ReadProvider
 * @see WriteProvider
 */
public interface CrudProvider<ID, INPUT, OUTPUT> extends ReadProvider<ID, OUTPUT>, WriteProvider<ID, INPUT, OUTPUT> {
}
