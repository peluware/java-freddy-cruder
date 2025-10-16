package com.peluware.freddy.cruder.reactive;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.concurrent.Flow;

/**
 * Flow-based contract for read-only operations on entities.
 * All results are exposed as {@link Flow.Publisher}, fully compatible
 * with the Reactive Streams specification.
 *
 * @param <E>  Entity type
 * @param <ID> Identifier type
 */
public interface FlowReadProvider<E, ID> {

    /**
     * Retrieves a paginated list of entities based on optional search criteria and query.
     *
     * @param search     optional search string (normalized before use)
     * @param pagination pagination settings, may be {@code null} for unpaginated
     * @param sort       sorting options, may be {@code null} for unsorted
     * @param query      additional query criteria, may be {@code null}
     * @return a {@link Flow.Publisher} emitting a single {@link Page} of entities
     */
    Flow.Publisher<Page<E>> page(String search, Pagination pagination, Sort sort, String query);

    /**
     * Finds an entity by its identifier.
     *
     * @param id the identifier of the entity
     * @return a {@link Flow.Publisher} emitting the found entity, or completing empty if not found
     */
    Flow.Publisher<E> find(@NotNull ID id);

    /**
     * Finds multiple entities by their identifiers.
     *
     * @param ids list of identifiers
     * @return a {@link Flow.Publisher} emitting the found entities (may complete empty if none exist)
     */
    Flow.Publisher<E> find(@NotNull @NotEmpty List<ID> ids);

    /**
     * Counts the number of entities matching the given search and query criteria.
     *
     * @param search optional search string (normalized before use)
     * @param query  additional query criteria, may be {@code null}
     * @return a {@link Flow.Publisher} emitting a single {@link Long} with the number of matches
     */
    Flow.Publisher<Long> count(String search, String query);

    /**
     * Checks if an entity exists by its identifier.
     *
     * @param id the identifier of the entity
     * @return a {@link Flow.Publisher} emitting {@code true} if the entity exists, {@code false} otherwise
     */
    Flow.Publisher<Boolean> exists(@NotNull ID id);
}
