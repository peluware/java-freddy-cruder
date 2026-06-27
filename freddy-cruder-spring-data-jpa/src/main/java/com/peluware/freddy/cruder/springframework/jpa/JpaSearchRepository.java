package com.peluware.freddy.cruder.springframework.jpa;

import com.peluware.freddy.cruder.springframework.SearchRepository;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * JPA fragment interface for paginated search and count.
 *
 * <p>Extend alongside {@link org.springframework.data.jpa.repository.JpaRepository} and
 * Spring Data wires {@link DefaultJpaSearchRepository} automatically via {@code spring.factories}.</p>
 *
 * @param <T> the entity type
 */
public interface JpaSearchRepository<T> extends SearchRepository<T> {

    @Override
    Page<T> findAllBySearch(@Nullable String search, @Nullable String query, Pageable pageable);

    @Override
    long countBySearch(@Nullable String search, @Nullable String query);
}
