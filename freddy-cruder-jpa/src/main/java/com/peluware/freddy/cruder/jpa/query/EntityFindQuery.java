package com.peluware.freddy.cruder.jpa.query;

import java.util.function.Supplier;

/**
 * {@link FindQuery} rooted at an entity class, returning the whole entity. Run it with
 * {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}:
 *
 * <pre>{@code
 * Product p = JpaQueryExecutor.exec(em, new EntityFindQuery<>(
 *     Product.class, byId, () -> new NotFoundEntityException(Product.class, id)));
 * }</pre>
 *
 * @param <T> the entity type
 */
public class EntityFindQuery<T> extends FindQuery<T, T> {

    /**
     * Returns the entity of {@code entityClass} matching {@code filter}, or throws {@code onEmpty}.
     *
     * @param entityClass the entity to find
     * @param filter      the predicate to match
     * @param onEmpty     supplies the exception thrown when no row matches
     */
    public EntityFindQuery(Class<T> entityClass, JpaPredicate<T> filter, Supplier<? extends RuntimeException> onEmpty) {
        super(entityClass, JpaSource.root(entityClass), JpaSelection.self(), filter, onEmpty);
    }
}
