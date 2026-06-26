package com.peluware.freddy.cruder;

import org.jspecify.annotations.Nullable;

/**
 * Provides counting of resources matching optional search and filter criteria.
 */
@FunctionalInterface
public interface CountProvider {

    /**
     * Counts the number of resources matching optional search and query filters.
     *
     * @param search optional text-based search (may be {@code null})
     * @param query  optional additional query expression (may be {@code null})
     * @return total number of resources matching the criteria
     */
    long count(@Nullable String search, @Nullable String query);
}
