package com.peluware.freddy.cruder;

import com.peluware.domain.Sort;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Provides an unpaginated listing of resources with optional search and filter criteria.
 *
 * <p>Intended for process-internal consumers — exports, reports, batch jobs — that need every
 * matching resource rather than a page of them. Not composed into {@link ReadProvider}: the
 * ability to paginate a resource does not imply that listing all of it is sensible; a concrete
 * provider opts into this separately.</p>
 *
 * @param <OUTPUT> the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface ListProvider<OUTPUT> {

    /**
     * Retrieves every resource matching the given search criteria and filtering expression.
     *
     * @param search optional text-based search (may be {@code null})
     * @param query  additional filtering expression, may be {@code null}
     * @param sort   sorting configuration, or {@code null} for unsorted results
     * @return every matching resource
     */
    List<OUTPUT> list(@Nullable String search, @Nullable String query, Sort sort);
}
