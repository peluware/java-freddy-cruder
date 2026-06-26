package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Provides paginated listing of resources scoped to a given owner.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface OwnedPageProvider<OWNER_ID, OUTPUT> {

    /**
     * Retrieves a paginated list of resources belonging to the given owner.
     *
     * @param ownerId    unique identifier of the owning resource
     * @param search     optional text-based search (may be {@code null})
     * @param query      additional filtering expression, may be {@code null}
     * @param pagination pagination settings, or {@code null} for unpaginated results
     * @param sort       sorting configuration, or {@code null} for unsorted results
     * @return a {@link Page} containing the paginated result set
     */
    Page<OUTPUT> page(@NotNull OWNER_ID ownerId, @Nullable String search, @Nullable String query, Pagination pagination, Sort sort);
}
