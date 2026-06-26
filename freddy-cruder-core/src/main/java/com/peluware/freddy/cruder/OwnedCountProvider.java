package com.peluware.freddy.cruder;

import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Provides counting of resources scoped to a given owner.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 */
@FunctionalInterface
public interface OwnedCountProvider<OWNER_ID> {

    /**
     * Counts resources belonging to the given owner matching optional filters.
     *
     * @param ownerId unique identifier of the owning resource
     * @param search  optional text-based search (may be {@code null})
     * @param query   optional additional query expression (may be {@code null})
     * @return total number of resources matching the criteria
     * @throws NotFoundException if the owner does not exist
     */
    long count(@NotNull OWNER_ID ownerId, @Nullable String search, @Nullable String query) throws NotFoundException;
}
