package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.ReadOnlyException;
import com.peluware.freddy.cruder.ReadOnlyProvider;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.jpa.query.EntityCountQuery;
import com.peluware.freddy.cruder.jpa.query.EntityExistsQuery;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.EntityListQuery;
import com.peluware.freddy.cruder.jpa.query.JpaOrder;
import com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor;
import com.peluware.freddy.cruder.jpa.query.JpaQueryHints;
import com.peluware.freddy.cruder.jpa.query.JpaResult;
import com.peluware.freddy.cruder.jpa.query.JpaResultHints;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
     * Finds an entity by its identifier, applying {@link #getQueryHints()} as the find properties, or
     * throws {@link NotFoundEntityException} if not found.
     */
    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        ENTITY entity = entityManager.<@Nullable ENTITY>find(entityClass, id, getQueryHints());
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
        var content = JpaQueryExecutor.exec(
            entityManager,
            new JpaQueryHints<>(
                new EntityListQuery<>(
                    entityClass,
                    searchPredicate(search, query),
                    sort,
                    pagination
                ),
                getQueryHints()
            )
        );
        return Page.deferred(
            content,
            pagination,
            sort,
            () -> internalCount(search, query)
        );
    }

    /**
     * Retrieves every entity matching the given search and query filters.
     */
    @Override
    protected List<ENTITY> internalList(@Nullable String search, @Nullable String query, Sort sort) {
        return JpaQueryExecutor.exec(
            entityManager,
            new JpaQueryHints<>(
                new EntityListQuery<>(
                    entityClass,
                    searchPredicate(search, query),
                    sort
                ),
                getQueryHints()
            )
        );
    }

    /**
     * Every entity matching the given search and query filters, lazily, through a live JPA cursor
     * ({@link jakarta.persistence.TypedQuery#getResultStream()}) — nothing is loaded until the
     * returned stream is consumed.
     */
    @Override
    protected Stream<ENTITY> internalStream(@Nullable String search, @Nullable String query, Sort sort) {
        return JpaTransactionRunners.installed().runStream(entityManager, () -> JpaQueryExecutor.exec(
            entityManager,
            entityClass,
            searchPredicate(search, query),
            JpaOrder.by(sort),
            new JpaResultHints<>(JpaResult.stream(), getQueryHints())
        ));
    }

    /**
     * Counts entities matching the given search and query filters.
     */
    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return JpaQueryExecutor.exec(
            entityManager,
            new JpaQueryHints<>(
                new EntityCountQuery<>(
                    entityClass,
                    searchPredicate(search, query)
                ),
                getQueryHints()
            )
        );
    }

    /**
     * Checks if an entity exists by its identifier.
     */
    @Override
    protected boolean internalExists(ID id) {
        return JpaQueryExecutor.exec(
            entityManager,
            new JpaQueryHints<>(
                new EntityExistsQuery<>(
                    entityClass,
                    buildIdPredicate(id)
                ),
                getQueryHints()
            )
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
        return JpaTransactionRunners.installed().run(entityManager, function);
    }

    protected JpaPredicate<ENTITY> buildIdPredicate(ID id) {
        return JpaPredicate.byId(entityManager.getMetamodel(), id);
    }

    /**
     * Returns JPA query hints to apply to every read executed by this provider, including
     * {@link EntityManager#find(Class, Object, java.util.Map) find}, which takes them as its properties.
     *
     * <p>
     * The default implementation returns an empty map. Subclasses may override to add hints such as
     * a fetch graph, cache control, fetch size or query timeout (see
     * {@link com.peluware.freddy.cruder.jpa.query.JpaHints}).
     * </p>
     *
     * <p>
     * In {@code find}, hints that only apply to queries (such as a query timeout) are ignored, and
     * none apply if the entity is already in the persistence context.
     * </p>
     *
     * @return a map of JPA query hints
     */
    protected Map<String, Object> getQueryHints() {
        return Map.of();
    }

    /**
     * The full-text search and RSQL predicate from the configured {@link SearchPredicateBuilder}.
     *
     * @param search normalized full-text search string, or {@code null}
     * @param query  RSQL query expression, or {@code null}
     * @return the search predicate
     */
    protected JpaPredicate<ENTITY> searchPredicate(@Nullable String search, @Nullable String query) {
        return searchPredicateBuilder.bind(entityManager.getMetamodel(), search, query);
    }

    /**
     * Read-only variant: {@code create}, {@code update} and {@code delete} throw
     * {@link ReadOnlyException}, and there is no input to map.
     */
    public abstract static class ReadOnly<ENTITY, ID, OUTPUT> extends JpaCrudProvider<ENTITY, ID, Void, OUTPUT> implements ReadOnlyProvider {

        public ReadOnly(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, Void> events) {
            super(entityManager, searchPredicateBuilder, entityClass, events);
        }

        public ReadOnly(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass) {
            super(entityManager, searchPredicateBuilder, entityClass);
        }

        public ReadOnly(EntityManager entityManager, Class<ENTITY> entityClass) {
            super(entityManager, entityClass);
        }

        public ReadOnly(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, EntityCrudEvents<ENTITY, ID, Void> events) {
            super(entityManager, searchPredicateBuilder, events);
        }

        public ReadOnly(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
            super(entityManager, searchPredicateBuilder);
        }

        public ReadOnly(EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected final void mapInput(Void input, ENTITY entity, boolean isNew) {
            throw new ReadOnlyException(getClass());
        }
    }
}
