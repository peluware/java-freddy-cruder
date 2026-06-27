package com.peluware.freddy.cruder.springframework.jpa;

import com.peluware.freddy.cruder.springframework.DefaultSearchRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.core.support.RepositoryMetadataAccess;

/**
 * Fragment implementation of {@link JpaSearchRepository} backed by {@link JpaSearchRepositoryEngine}.
 */
public class DefaultJpaSearchRepository<T> extends DefaultSearchRepository<T> implements JpaSearchRepository<T>, RepositoryMetadataAccess {

    public DefaultJpaSearchRepository(JpaSearchRepositoryEngine engine) {
        super(engine);
    }

    @Override
    public Page<T> findAllBySearch(@Nullable String search, @Nullable String query, Pageable pageable) {
        return super.findAllBySearch(search, query, pageable);
    }

    @Override
    public long countBySearch(@Nullable String search, @Nullable String query) {
        return super.countBySearch(search, query);
    }
}
