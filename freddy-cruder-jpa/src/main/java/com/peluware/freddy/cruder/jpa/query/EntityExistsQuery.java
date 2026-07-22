package com.peluware.freddy.cruder.jpa.query;

/**
 * {@link ExistsQuery} rooted at an entity class. Run it with
 * {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}:
 *
 * <pre>{@code
 * boolean any = JpaQueryExecutor.exec(em, new EntityExistsQuery<>(Product.class, filter));
 * }</pre>
 *
 * @param <T> the entity type
 */
public class EntityExistsQuery<T> extends ExistsQuery<T> {

    /**
     * Whether {@code entityClass} has any row.
     *
     * @param entityClass the entity to probe
     */
    public EntityExistsQuery(Class<T> entityClass) {
        super(JpaSource.root(entityClass));
    }

    /**
     * Whether {@code entityClass} has any row matching {@code filter}.
     *
     * @param entityClass the entity to probe
     * @param filter      the predicate to match
     */
    public EntityExistsQuery(Class<T> entityClass, JpaPredicate<T> filter) {
        super(JpaSource.root(entityClass), filter);
    }
}
