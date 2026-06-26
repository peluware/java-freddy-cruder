package com.peluware.freddy.cruder;

import jakarta.validation.constraints.NotNull;

/**
 * Provides deletion of a resource within the scope of a given owner.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the identifier type of the resource
 */
@FunctionalInterface
public interface OwnedDeleteProvider<OWNER_ID, ID> {

    /**
     * Deletes a resource within the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource to delete
     * @throws NotFoundException if the owner or resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    void delete(@NotNull OWNER_ID ownerId, @NotNull ID id) throws NotFoundException;
}
