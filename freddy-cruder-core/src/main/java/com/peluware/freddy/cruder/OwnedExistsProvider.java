package com.peluware.freddy.cruder;

import jakarta.validation.constraints.NotNull;

/**
 * Provides existence checks for a resource scoped to a given owner.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the identifier type of the resource
 */
@FunctionalInterface
public interface OwnedExistsProvider<OWNER_ID, ID> {

    /**
     * Checks whether a resource with the given identifier exists within
     * the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @return {@code true} if the resource exists and belongs to the owner,
     *         otherwise {@code false}
     */
    boolean exists(@NotNull OWNER_ID ownerId, @NotNull ID id);
}
