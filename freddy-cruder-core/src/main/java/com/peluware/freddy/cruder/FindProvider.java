package com.peluware.freddy.cruder;

import jakarta.validation.constraints.NotNull;

/**
 * Provides retrieval of a single resource by its identifier.
 *
 * @param <ID>     the identifier type of the resource
 * @param <OUTPUT> the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface FindProvider<ID, OUTPUT> {

    /**
     * Finds a resource by its identifier.
     *
     * @param id unique identifier of the resource
     * @return the resource mapped to its output representation
     * @throws NotFoundException if no resource exists with the given identifier
     */
    OUTPUT find(@NotNull ID id) throws NotFoundException;
}
