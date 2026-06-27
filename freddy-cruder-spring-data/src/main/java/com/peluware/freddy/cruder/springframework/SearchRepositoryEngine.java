package com.peluware.freddy.cruder.springframework;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Strategy for executing search and count queries against a specific store.
 *
 * <p>Receives the domain type at runtime so a single engine instance can serve
 * all repositories. Injected into {@link DefaultSearchRepository} and its
 * store-specific subclasses.</p>
 */
public interface SearchRepositoryEngine {

    <T> Page<T> findAllBySearch(Class<T> domainType, @Nullable String search, @Nullable String query, Pageable pageable);

    <T> long countBySearch(Class<T> domainType, @Nullable String search, @Nullable String query);
}
