package com.peluware.freddy.cruder.springframework.jpa;

import com.peluware.freddy.cruder.springframework.DefaultSearchRepository;
import org.springframework.data.repository.core.support.RepositoryMetadataAccess;

/**
 * Fragment implementation of {@link JpaSearchRepository} backed by {@link JpaSearchEngine}.
 */
public class DefaultJpaSearchRepository<T> extends DefaultSearchRepository<T> implements JpaSearchRepository<T>, RepositoryMetadataAccess {

    public DefaultJpaSearchRepository(JpaSearchEngine engine) {
        super(engine);
    }
}
