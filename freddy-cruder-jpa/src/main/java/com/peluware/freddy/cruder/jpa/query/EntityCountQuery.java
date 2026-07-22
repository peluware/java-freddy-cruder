package com.peluware.freddy.cruder.jpa.query;

/**
 * {@link CountQuery} rooted at an entity class. Run it with
 * {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}:
 *
 * <pre>{@code
 * long total = JpaQueryExecutor.exec(em, new EntityCountQuery<>(Product.class, filter));
 * }</pre>
 *
 * @param <T> the entity type
 */
public class EntityCountQuery<T> extends CountQuery<T> {

    /**
     * Counts every row of {@code entityClass}.
     *
     * @param entityClass the entity to count
     */
    public EntityCountQuery(Class<T> entityClass) {
        super(JpaSource.root(entityClass));
    }

    /**
     * Counts the rows of {@code entityClass} matching {@code filter}.
     *
     * @param entityClass the entity to count
     * @param filter      the predicate to match
     */
    public EntityCountQuery(Class<T> entityClass, JpaPredicate<T> filter) {
        super(JpaSource.root(entityClass), filter);
    }
}
