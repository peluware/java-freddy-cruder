package com.peluware.freddy.cruder.springframework;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.RepositoryMethodContext;
import org.springframework.data.repository.core.support.RepositoryMetadataAccess;

/**
 * Base fragment implementation of {@link SearchRepository} that delegates to a {@link SearchRepositoryEngine}.
 *
 * <p>Resolves the domain type at runtime via {@link RepositoryMethodContext} so a single
 * instance can serve any repository without generics erasure issues.</p>
 */
public class DefaultSearchRepository<T> implements SearchRepository<T>, RepositoryMetadataAccess {

    private final SearchRepositoryEngine engine;

    public DefaultSearchRepository(SearchRepositoryEngine engine) {
        this.engine = engine;
    }

    @Override
    public Page<T> findAllBySearch(@Nullable String search, @Nullable String query, Pageable pageable) {
        @SuppressWarnings("unchecked")
        Class<T> domainType = (Class<T>) RepositoryMethodContext.getContext().getMetadata().getDomainType();
        return engine.findAllBySearch(domainType, search, query, pageable);
    }

    @Override
    public long countBySearch(@Nullable String search, @Nullable String query) {
        @SuppressWarnings("unchecked")
        Class<T> domainType = (Class<T>) RepositoryMethodContext.getContext().getMetadata().getDomainType();
        return engine.countBySearch(domainType, search, query);
    }
}
