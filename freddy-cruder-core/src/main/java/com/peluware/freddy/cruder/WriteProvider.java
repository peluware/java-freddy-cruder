package com.peluware.freddy.cruder;

import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Defines the contract for write operations on entities (create, update, delete).
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface WriteProvider<E, D, ID> {

    /**
     * Creates a new entity based on the given DTO.
     *
     * @param dto the data transfer object containing creation data
     * @return the newly created entity
     */
    E create(@NotNull @Valid D dto);

    /**
     * Updates an existing entity identified by the given ID using the provided DTO.
     *
     * @param id  the identifier of the entity to update
     * @param dto the data transfer object containing updated data
     * @return the updated entity
     * @throws NotFoundEntityException if the entity with the given ID does not exist
     */
    E update(@NotNull ID id, @NotNull @Valid D dto) throws NotFoundEntityException;

    /**
     * Deletes an entity identified by the given ID.
     *
     * @param id the identifier of the entity to delete
     * @throws NotFoundEntityException if the entity with the given ID does not exist
     */
    void delete(@NotNull ID id) throws NotFoundEntityException;
}

