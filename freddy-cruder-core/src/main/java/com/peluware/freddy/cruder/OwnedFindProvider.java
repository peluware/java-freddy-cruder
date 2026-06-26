package com.peluware.freddy.cruder;

import jakarta.validation.constraints.NotNull;

/**
 * Provides retrieval of a single resource by owner and identifier.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the identifier type of the resource
 * @param <OUTPUT>   the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface OwnedFindProvider<OWNER_ID, ID, OUTPUT> {

    /**
     * Finds a resource by its identifier within the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @return the resource mapped to its output representation
     * @throws NotFoundException if the owner or resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    OUTPUT find(@NotNull OWNER_ID ownerId, @NotNull ID id) throws NotFoundException;
}
