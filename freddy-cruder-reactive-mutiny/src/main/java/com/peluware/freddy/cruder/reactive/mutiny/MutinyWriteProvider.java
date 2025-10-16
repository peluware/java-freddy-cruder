package com.peluware.freddy.cruder.reactive.mutiny;

import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Mutiny contract for write operations on entities (create, update, delete).
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface MutinyWriteProvider<E, D, ID> {

    /**
     * Creates a new entity based on the given DTO.
     *
     * @param dto the data transfer object containing creation data
     * @return a {@link Uni} emitting the newly created entity
     */
    Uni<E> create(@NotNull @Valid D dto);

    /**
     * Updates an existing entity identified by the given ID using the provided DTO.
     *
     * @param id  the identifier of the entity to update
     * @param dto the data transfer object containing updated data
     * @return a {@link Uni} emitting the updated entity or failing with {@link NotFoundEntityException} if not found
     */
    Uni<E> update(@NotNull ID id, @NotNull @Valid D dto);

    /**
     * Deletes an entity identified by the given ID.
     *
     * @param id the identifier of the entity to delete
     * @return a {@link Uni} emitting {@code null} on successful completion, or failing with {@link NotFoundEntityException} if not found
     */
    Uni<Void> delete(@NotNull ID id);
}
