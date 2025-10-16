package com.peluware.freddy.cruder.reactive;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.concurrent.Flow;

/**
 * Flow-based contract for write operations on entities (create, update, delete).
 * All results are exposed as {@link Flow.Publisher}, compatible with Reactive Streams.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface FlowWriteProvider<E, D, ID> {

    /**
     * Creates a new entity based on the given DTO.
     *
     * @param dto the data transfer object containing creation data
     * @return a {@link Flow.Publisher} emitting the newly created entity
     */
    Flow.Publisher<E> create(@NotNull @Valid D dto);

    /**
     * Updates an existing entity identified by the given ID using the provided DTO.
     *
     * @param id  the identifier of the entity to update
     * @param dto the data transfer object containing updated data
     * @return a {@link Flow.Publisher} emitting the updated entity,
     * or completing empty if not found
     */
    Flow.Publisher<E> update(@NotNull ID id, @NotNull @Valid D dto);

    /**
     * Deletes an entity identified by the given ID.
     *
     * @param id the identifier of the entity to delete
     * @return a {@link Flow.Publisher} completing when the entity is deleted,
     * or completing empty if not found
     */
    Flow.Publisher<Void> delete(@NotNull ID id);
}
