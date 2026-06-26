package com.peluware.freddy.cruder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Provides update of an existing resource identified by its ID.
 *
 * @param <ID>     the identifier type of the resource
 * @param <INPUT>  the input DTO type used to update the resource
 * @param <OUTPUT> the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface UpdateProvider<ID, INPUT, OUTPUT> {

    /**
     * Updates an existing resource identified by the given ID.
     *
     * @param id    unique identifier of the resource to update
     * @param input input DTO containing updated data
     * @return the updated resource mapped to its output representation
     * @throws NotFoundException if no resource exists with the given ID
     */
    OUTPUT update(@NotNull ID id, @NotNull @Valid INPUT input) throws NotFoundException;
}
