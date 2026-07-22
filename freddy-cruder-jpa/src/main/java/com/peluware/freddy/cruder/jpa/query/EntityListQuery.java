package com.peluware.freddy.cruder.jpa.query;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;

/**
 * Reusable list query over whole entities of a class — the {@link ListQuery} specialization where
 * the row type is the entity itself ({@code R = T}), rooted at the class. Run it with
 * {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}:
 *
 * <pre>{@code
 * List<Product> page = JpaQueryExecutor.exec(em,
 *     new EntityListQuery<>(Product.class, filter, sort, pagination));
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
     * @param sort        the sort specification
     * @param pagination  the pagination specification
     */
    public EntityListQuery(Class<T> entityClass, JpaPredicate<T> filter, Sort sort, Pagination pagination) {
        super(entityClass, JpaSource.root(entityClass), JpaSelection.self(), filter, sort, pagination);
    }

    /**
     * Lists the entities matching {@code filter}, sorted, without pagination.
     *
     * @param entityClass the entity to list
     * @param filter      the predicate to match
     * @param sort        the sort specification
     */
    public EntityListQuery(Class<T> entityClass, JpaPredicate<T> filter, Sort sort) {
        this(entityClass, filter, sort, Pagination.unpaginated());
    }

    /**
     * Lists the entities matching {@code filter}, paginated, without ordering.
     *
     * @param entityClass the entity to list
     * @param filter      the predicate to match
     * @param pagination  the pagination specification
     */
    public EntityListQuery(Class<T> entityClass, JpaPredicate<T> filter, Pagination pagination) {
        this(entityClass, filter, Sort.unsorted(), pagination);
    }

    /**
     * Lists every entity matching {@code filter}, unsorted and unpaginated.
     *
     * @param entityClass the entity to list
     * @param filter      the predicate to match
     */
    public EntityListQuery(Class<T> entityClass, JpaPredicate<T> filter) {
        this(entityClass, filter, Sort.unsorted(), Pagination.unpaginated());
    }

    /**
     * Lists every entity of {@code entityClass}, sorted and paginated.
     *
     * @param entityClass the entity to list
     * @param sort        the sort specification
     * @param pagination  the pagination specification
     */
    public EntityListQuery(Class<T> entityClass, Sort sort, Pagination pagination) {
        this(entityClass, JpaPredicate.all(), sort, pagination);
    }

    /**
     * Lists every entity of {@code entityClass}, sorted, without pagination.
     *
     * @param entityClass the entity to list
     * @param sort        the sort specification
     */
    public EntityListQuery(Class<T> entityClass, Sort sort) {
        this(entityClass, JpaPredicate.all(), sort, Pagination.unpaginated());
    }

    /**
     * Lists every entity of {@code entityClass}, paginated, without ordering.
     *
     * @param entityClass the entity to list
     * @param pagination  the pagination specification
     */
    public EntityListQuery(Class<T> entityClass, Pagination pagination) {
        this(entityClass, JpaPredicate.all(), Sort.unsorted(), pagination);
    }

    /**
     * Lists every entity of {@code entityClass}, unsorted and unpaginated.
     *
     * @param entityClass the entity to list
     */
    public EntityListQuery(Class<T> entityClass) {
        this(entityClass, JpaPredicate.all(), Sort.unsorted(), Pagination.unpaginated());
    }
}
