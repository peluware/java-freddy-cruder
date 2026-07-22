package com.peluware.freddy.cruder.springframework.mongodb;

import com.peluware.freddy.cruder.springframework.SearchRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * MongoDB fragment interface for paginated search and count.
 *
 * <p>Extend alongside {@link org.springframework.data.mongodb.repository.MongoRepository} and
 * Spring Data wires {@link DefaultMongoSearchRepository} automatically via {@code spring.factories}.</p>
 *
 * @param <T> the document type
 */
public interface MongoSearchRepository<T> extends SearchRepository<T> {

    @Override
    Page<T> findAllBySearch(@Nullable String search, @Nullable String query, Pageable pageable);

    @Override
    long countBySearch(@Nullable String search, @Nullable String query);
}
