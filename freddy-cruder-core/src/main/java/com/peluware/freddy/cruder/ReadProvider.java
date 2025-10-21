package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import jakarta.validation.constraints.NotNull;

/**
 * Defines the contract for read-only operations on entities.
 *
 * @param <E>  Entity type
 * @param <ID> Identifier type
 */
public interface ReadProvider<E, ID> {

    /**
     * Retrieves a paginated list of entities based on optional search criteria and query.
     *
     * @param search     optional search string (normalized before use)
     * @param pagination pagination settings, may be {@code null} for unpaginated
     * @param sort       sorting options, may be {@code null} for unsorted
     * @param query      additional query criteria, may be {@code null}
     * @return a {@link Page} of entities
     */
    Page<E> page(String search, Pagination pagination, Sort sort, String query);

    /**
     * Finds an entity by its identifier.
     *
     * @param id the identifier of the entity
     * @return the found entity
     * @throws NotFoundEntityException if the entity does not exist
     */
    E find(@NotNull ID id) throws NotFoundEntityException;

    /**
     * Counts the number of entities matching the given search and query criteria.
     *
     * @param search optional search string (normalized before use)
     * @param query  additional query criteria, may be {@code null}
     * @return the number of matching entities
     */
    long count(String search, String query);

    /**
     * Checks if an entity exists by its identifier.
     *
     * @param id the identifier of the entity
     * @return {@code true} if the entity exists, {@code false} otherwise
     */
    boolean exists(@NotNull ID id);
}
