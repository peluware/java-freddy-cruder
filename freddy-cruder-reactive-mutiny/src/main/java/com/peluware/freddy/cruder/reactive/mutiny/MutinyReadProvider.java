package com.peluware.freddy.cruder.reactive.mutiny;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Mutiny contract for read-only operations on entities.
 *
 * @param <E>  Entity type
 * @param <ID> Identifier type
 */
public interface MutinyReadProvider<E, ID> {

    /**
     * Retrieves a reactive paginated list of entities based on optional search criteria and query.
     *
     * @param search     optional search string (normalized before use)
     * @param pagination pagination settings, may be {@code null} for unpaginated
     * @param sort       sorting options, may be {@code null} for unsorted
     * @param query      additional query criteria, may be {@code null}
     * @return a {@link Uni} emitting a {@link Page} of entities
     */
    Uni<Page<E>> page(String search, Pagination pagination, Sort sort, String query);

    /**
     * Finds an entity by its identifier.
     *
     * @param id the identifier of the entity
     * @return a {@link Uni} emitting the found entity or failing with {@link NotFoundEntityException} if not found
     */
    Uni<E> find(@NotNull ID id);

    /**
     * Finds multiple entities by their identifiers.
     *
     * @param ids list of identifiers
     * @return a {@link Uni} emitting the found entities
     */
    Uni<List<E>> find(@NotNull @NotEmpty List<ID> ids);

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
}
