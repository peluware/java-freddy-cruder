package com.peluware.freddy.cruder;

import jakarta.validation.constraints.NotNull;

/**
 * Provides deletion of a resource by its identifier.
 *
 * @param <ID> the identifier type of the resource
 */
@FunctionalInterface
public interface DeleteProvider<ID> {

    /**
     * Deletes the resource identified by the given ID.
     *
     * @param id unique identifier of the resource to delete
     * @throws NotFoundException if the resource does not exist
     */
    void delete(@NotNull ID id) throws NotFoundException;
}
