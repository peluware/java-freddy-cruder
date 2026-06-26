package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import org.jspecify.annotations.Nullable;

/**
 * Provides paginated listing of resources with optional search and filter criteria.
 *
 * @param <OUTPUT> the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface PageProvider<OUTPUT> {

    /**
     * Retrieves a paginated list of resources based on optional search criteria
     * and filtering expression.
     *
     * @param search     optional text-based search (may be {@code null})
     * @param query      additional filtering expression, may be {@code null}
     * @param pagination pagination settings, or {@code null} for unpaginated results
     * @param sort       sorting configuration, or {@code null} for unsorted results
     * @return a {@link Page} containing the paginated result set
     */
    Page<OUTPUT> page(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort);
}
