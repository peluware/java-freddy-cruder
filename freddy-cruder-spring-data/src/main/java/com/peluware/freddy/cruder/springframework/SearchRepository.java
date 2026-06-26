package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Abstraction for search and count operations over a collection of entities.
 *
 * <p>
 * This interface decouples {@link SpringRepositoryCrudProvider} from any specific
 * search library. Implement it directly, or use {@link OmniSearchRepository} to
 * bridge an existing {@code omni-search} implementation.
 * </p>
 *
 * @param <T> the domain type the repository manages
 * @see OmniSearchRepository
 * @see CrudSearchRepository
 */
public interface SearchRepository<T> {

    /**
     * Returns a page of entities matching the given search and RSQL filter.
     *
     * <p>By default, delegates to {@link #findBySearch(String, String, Pageable)} by converting
     * the peluware pagination and sort into a Spring {@link Pageable}. Override this method
     * directly when working outside of Spring Data (e.g. a manual JPA implementation).</p>
     *
     * <p><strong>Note:</strong> this method and {@link #findBySearch(String, String, Pageable)}
     * delegate to each other by default. At least one of the two must be overridden;
     * failing to do so will result in a {@link StackOverflowError} at runtime.</p>
     *
     * @param search     normalized full-text search string, or {@code null}
     * @param query      RSQL filter expression, or {@code null}
     * @param pagination pagination settings
     * @param sort       sorting configuration
     * @return a page of matching entities
     */
    default com.peluware.domain.Page<T> findBySearch(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        return SpringToPeluwareAdapters.applyAsPage(pagination, sort, pageable -> findBySearch(search, query, pageable));
    }

    /**
     * Returns a Spring {@link Page} of entities matching the given search and RSQL filter,
     * bridging Spring's {@link Pageable} into peluware's pagination model.
     *
     * <p>By default, delegates to {@link #findBySearch(String, String, Pagination, Sort)} by converting
     * the {@link Pageable} into peluware types. Override this method when using Spring Data's
     * {@code @Query} or derived query mechanisms, which only understand {@link Pageable}.</p>
     *
     * <p><strong>Note:</strong> this method and {@link #findBySearch(String, String, Pagination, Sort)}
     * delegate to each other by default. At least one of the two must be overridden;
     * failing to do so will result in a {@link StackOverflowError} at runtime.</p>
     *
     * @param search   normalized full-text search string, or {@code null}
     * @param query    RSQL filter expression, or {@code null}
     * @param pageable Spring pagination and sort settings
     * @return a Spring {@link Page} of matching entities
     */
    default Page<T> findBySearch(@Nullable String search, @Nullable String query, Pageable pageable) {
        return PeluwareToSpringAdapters.applyAsPage(pageable, (pagination, sort) -> findBySearch(search, query, pagination, sort));
    }

    /**
     * Counts entities matching the given search and RSQL filter.
     *
     * @param search normalized full-text search string, or {@code null}
     * @param query  RSQL filter expression, or {@code null}
     * @return total count of matching entities
     */
    long countBySearch(@Nullable String search, @Nullable String query);
}
