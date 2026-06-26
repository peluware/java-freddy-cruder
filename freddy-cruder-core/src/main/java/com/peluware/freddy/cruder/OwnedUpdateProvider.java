package com.peluware.freddy.cruder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Provides update of an existing resource within the scope of a given owner.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the identifier type of the resource
 * @param <INPUT>    the input DTO type used to update the resource
 * @param <OUTPUT>   the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface OwnedUpdateProvider<OWNER_ID, ID, INPUT, OUTPUT> {

    /**
     * Updates an existing resource within the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource to update
     * @param input   input DTO containing updated data
     * @return the updated resource mapped to its output representation
     * @throws NotFoundException if the owner or resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    OUTPUT update(@NotNull OWNER_ID ownerId, @NotNull ID id, @NotNull @Valid INPUT input) throws NotFoundException;
}
