package com.peluware.freddy.cruder.reactive.mutiny;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Generic reactive CRUD provider abstraction using Mutiny, designed to decouple
 * application logic from persistence details. Implementations define how entities
 * are created, retrieved, updated, deleted and listed in a non-blocking manner,
 * without imposing restrictions on underlying frameworks or technology.
 *
 * @param <ID>     Type of the unique identifier of the resource.
 * @param <INPUT>  Type of the input DTO used for creating or updating resources.
 * @param <OUTPUT> Type of the output DTO returned to the consumer.
 */
public interface MutinyCrudProvider<ID, INPUT, OUTPUT> {

    /**
     * Retrieves a reactive paginated list of entities based on optional search criteria and query.
     *
     * @param search     optional search string (normalized before use)
     * @param pagination pagination settings, may be {@code null} for unpaginated
     * @param sort       sorting options, may be {@code null} for unsorted
     * @param query      additional query criteria, may be {@code null}
     * @return a {@link Uni} emitting a {@link Page} of entities
     */
    Uni<Page<OUTPUT>> page(String search, Pagination pagination, Sort sort, String query);

    /**
     * Finds an entity by its identifier.
     *
     * @param id the identifier of the entity
     * @return a {@link Uni} emitting the found entity or failing with {@link NotFoundEntityException} if not found
     */
    Uni<OUTPUT> find(@NotNull ID id);

    /**
     * Counts the number of entities matching the given search and query criteria.
     *
     * @param search optional search string (normalized before use)
     * @param query  additional query criteria, may be {@code null}
     * @return a {@link Uni} emitting the number of matching entities
     */
    Uni<Long> count(String search, String query);

    /**
     * Checks if an entity exists by its identifier.
     *
     * @param id the identifier of the entity
     * @return a {@link Uni} emitting {@code true} if the entity exists, {@code false} otherwise
     */
    Uni<Boolean> exists(@NotNull ID id);


    /**
     * Creates a new entity based on the given DTO.
     *
     * @param dto the data transfer object containing creation data
     * @return a {@link Uni} emitting the newly created entity
     */
    Uni<OUTPUT> create(@NotNull @Valid INPUT dto);

    /**
     * Updates an existing entity identified by the given ID using the provided DTO.
     *
     * @param id  the identifier of the entity to update
     * @param dto the data transfer object containing updated data
     * @return a {@link Uni} emitting the updated entity or failing with {@link NotFoundEntityException} if not found
     */
    Uni<OUTPUT> update(@NotNull ID id, @NotNull @Valid INPUT dto);

    /**
     * Deletes an entity identified by the given ID.
     *
     * @param id the identifier of the entity to delete
     * @return a {@link Uni} emitting {@code null} on successful completion, or failing with {@link NotFoundEntityException} if not found
     */
    Uni<Void> delete(@NotNull ID id);

}
