package com.peluware.freddy.cruder.springframework.mongodb;

import com.peluware.freddy.cruder.springframework.DefaultSearchRepository;
import org.springframework.data.repository.core.support.RepositoryMetadataAccess;


/**
 * Fragment implementation of {@link MongoSearchRepository} backed by {@link MongoSearchEngine}.
 */
public class DefaultMongoSearchRepository<T> extends DefaultSearchRepository<T> implements MongoSearchRepository<T>, RepositoryMetadataAccess {

    public DefaultMongoSearchRepository(MongoSearchEngine engine) {
        super(engine);
    }
}
