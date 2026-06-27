package com.peluware.freddy.cruder.springframework;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Fragment interface that adds paginated search and count operations to a Spring Data repository.
 *
 * <p>Store-specific modules register their own sub-interface and implementation via
 * {@code spring.factories}. For JPA, extend {@code JpaSearchRepository} instead of this
 * interface directly.</p>
 *
 * @param <T> the entity type
 */
public interface SearchRepository<T> {

    default Page<T> findAllBySearch(@Nullable String search, @Nullable String query, Pageable pageable) {
        throw new UnsupportedOperationException("findBySearch requires a store-specific fragment implementation");
    }

    default long countBySearch(@Nullable String search, @Nullable String query) {
        throw new UnsupportedOperationException("countBySearch requires a store-specific fragment implementation");
    }
}
