package com.peluware.freddy.cruder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Provides creation of a new resource under a given owner.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <INPUT>    the input DTO type used to create the resource
 * @param <OUTPUT>   the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface OwnedCreateProvider<OWNER_ID, INPUT, OUTPUT> {

    /**
     * Creates a new resource under the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param input   input DTO containing creation data
     * @return the newly created resource mapped to its output representation
     * @throws NotFoundException if the owner does not exist
     */
    OUTPUT create(@NotNull OWNER_ID ownerId, @NotNull @Valid INPUT input) throws NotFoundException;
}
