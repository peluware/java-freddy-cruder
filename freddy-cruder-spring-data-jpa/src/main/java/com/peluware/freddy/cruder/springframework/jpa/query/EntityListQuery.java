package com.peluware.freddy.cruder.springframework.jpa.query;

import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.JpaSelection;
import com.peluware.freddy.cruder.jpa.query.JpaSource;
import org.springframework.data.domain.Pageable;

/**
 * Spring Data flavored {@link ListQuery} over whole entities of a class — the specialization where
 * the row type is the entity itself ({@code R = T}), rooted at the class, sorted and paginated by a
 * {@link Pageable}. Run it with
 * {@link com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor#exec(jakarta.persistence.EntityManager, com.peluware.freddy.cruder.jpa.query.JpaQuery)}:
 *
 * <pre>{@code
 * List<Product> page = JpaQueryExecutor.exec(em,
 *     new EntityListQuery<>(Product.class, filter, pageable));
 * }</pre>
 *
 * @param <T> the entity type
 */
public class EntityListQuery<T> extends ListQuery<T, T> {

    /**
     * Lists the entities of {@code entityClass} matching {@code filter}, sorted and paginated.
     *
     * @param entityClass the entity to list
     * @param filter      the predicate to match
     * @param pageable    the sort and pagination specification
     */
    public EntityListQuery(Class<T> entityClass, JpaPredicate<T> filter, Pageable pageable) {
        super(entityClass, JpaSource.root(entityClass), JpaSelection.self(), filter, pageable);
    }

    /**
     * Lists every entity of {@code entityClass}, sorted and paginated.
     *
     * @param entityClass the entity to list
     * @param pageable    the sort and pagination specification
     */
    public EntityListQuery(Class<T> entityClass, Pageable pageable) {
        this(entityClass, JpaPredicate.all(), pageable);
    }

    /**
     * Lists every entity matching {@code filter}, unsorted and unpaginated.
     *
     * @param entityClass the entity to list
     * @param filter      the predicate to match
     */
    public EntityListQuery(Class<T> entityClass, JpaPredicate<T> filter) {
        this(entityClass, filter, Pageable.unpaged());
    }

    /**
     * Lists every entity of {@code entityClass}, unsorted and unpaginated.
     *
     * @param entityClass the entity to list
     */
    public EntityListQuery(Class<T> entityClass) {
        this(entityClass, JpaPredicate.all(), Pageable.unpaged());
    }
}
