package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.NotFoundEntityException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * JPA-specific implementation of {@link EntityCrudProvider} that provides
 * standard CRUD operations for JPA entities using an {@link EntityManager}.
 *
 * <p>
 * This class delegates search and pagination to a {@link SearchPredicateBuilder},
 * building all read queries through the JPA Criteria API. The default predicate
 * builder wraps {@code omni-search-jpa} via {@link OmniSearchPredicateAdapter};
 * provide a custom {@link SearchPredicateBuilder} to use a different strategy.
 * </p>
 *
 * <p>
 * For use cases that require a global predicate filter (soft-delete, multi-tenancy,
 * row-level security), prefer {@link FilterableJpaCrudProvider}, which exposes the
 * {@code predicateFilter} hook and full Criteria API control over every query.
 * </p>
 *
 * @param <ENTITY> the JPA entity type
 * @param <ID>     the entity identifier type
 * @param <INPUT>  the input DTO type for create/update operations
 * @param <OUTPUT> the output DTO or projection type
 */
public abstract class JpaCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final EntityManager entityManager;
    protected final SearchPredicateBuilder searchPredicateBuilder;

    // ------------------------------------------------------------
    // CONSTRUCTORS — explicit entityClass
    // ------------------------------------------------------------

    /**
     * Creates a JPA CRUD provider with explicit entity class, custom search predicate builder and lifecycle events.
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param entityClass            the entity class managed by this provider
     * @param events                 the CRUD lifecycle events handler
     */
    public JpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
    }

    /**
     * Creates a JPA CRUD provider with explicit entity class and custom search predicate builder,
     * using default lifecycle events (no-op).
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param entityClass            the entity class managed by this provider
     */
    public JpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass) {
        this(entityManager, searchPredicateBuilder, entityClass, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a JPA CRUD provider with explicit entity class, using the default
     * {@link OmniSearchPredicateAdapter} and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     * @param entityClass   the entity class managed by this provider
     */
    public JpaCrudProvider(EntityManager entityManager, Class<ENTITY> entityClass) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault(), entityClass);
    }

    // ------------------------------------------------------------
    // CONSTRUCTORS — reflection-based entityClass
    // ------------------------------------------------------------

    /**
     * Creates a JPA CRUD provider by resolving the entity class automatically from the
     * generic type hierarchy via reflection, using a custom search predicate builder and lifecycle events.
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param events                 the CRUD lifecycle events handler
     */
    public JpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
    }

    /**
     * Creates a JPA CRUD provider by resolving the entity class automatically from the
     * generic type hierarchy via reflection, using a custom search predicate builder
     * and default lifecycle events (no-op).
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     */
    public JpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        this(entityManager, searchPredicateBuilder, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a JPA CRUD provider by resolving the entity class automatically from the
     * generic type hierarchy via reflection, using the default {@link OmniSearchPredicateAdapter}
     * and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     */
    public JpaCrudProvider(EntityManager entityManager) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault());
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    /**
     * Finds an entity by its identifier or throws {@link NotFoundEntityException} if not found.
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        ENTITY entity = entityManager.find(entityClass, id);
        if (entity == null) {
            throw new NotFoundEntityException(entityClass, id);
        }
        return entity;
    }

    /**
     * Retrieves a paginated list of entities matching the given search and query filters.
     * Uses a deferred count strategy — the total count is only resolved if needed.
     */
    @Override
    protected Page<ENTITY> internalPage(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = JpaQueryHelpers.query(
            entityManager,
            entityClass,
            entityClass,
            (root, cb) -> searchPredicateBuilder.build(root, cb, entityManager.getMetamodel(), search, query),
            JpaCriteriaExecutor.list(sort, pagination)
        );
        return Page.deferred(
            content,
            pagination,
            sort,
            () -> internalCount(search, query)
        );
    }

    /**
     * Counts entities matching the given search and query filters.
     */
    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return JpaQueryHelpers.query(
            entityManager,
            entityClass,
            Long.class,
            (root, cb) -> searchPredicateBuilder.build(root, cb, entityManager.getMetamodel(), search, query),
            JpaCriteriaExecutor.count()
        );
    }

    /**
     * Checks if an entity exists by its identifier.
     */
    @Override
    protected boolean internalExists(ID id) {
        return JpaQueryHelpers.query(
            entityManager,
            entityClass,
            Long.class,
            (root, cb) -> buildIdPredicate(root, cb, id),
            JpaCriteriaExecutor.exists()
        );
    }

    /**
     * Persists a new entity.
     */
    @Override
    protected ENTITY internalCreate(ENTITY entity) {
        entityManager.persist(entity);
        return entity;
    }

    /**
     * Updates an existing entity.
     */
    @Override
    protected ENTITY internalUpdate(ENTITY entity) {
        return entityManager.merge(entity);
    }

    /**
     * Deletes an entity.
     */
    @Override
    protected void internalDelete(ENTITY entity) {
        entityManager.remove(entity);
    }

    /**
     * Executes a function within a transaction.
     */
    @Override
    protected <T> T withTransaction(Supplier<T> function) {
        return JpaUtils.requireTransaction(entityManager, function);
    }

    protected Predicate buildIdPredicate(Root<ENTITY> root, CriteriaBuilder cb, ID id) {
        return cb.equal(root.get(JpaUtils.getIdFieldName(entityManager.getMetamodel(), entityClass)), id);
    }

}
