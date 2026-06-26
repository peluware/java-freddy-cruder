package com.peluware.freddy.cruder;

import jakarta.validation.constraints.NotNull;

/**
 * Provides existence checks for a resource by its identifier.
 *
 * @param <ID> the identifier type of the resource
 */
@FunctionalInterface
public interface ExistsProvider<ID> {

    /**
     * Checks whether a resource with the given identifier exists.
     *
     * @param id unique identifier of the resource
     * @return {@code true} if the resource exists, otherwise {@code false}
     */
    boolean exists(@NotNull ID id);
}
