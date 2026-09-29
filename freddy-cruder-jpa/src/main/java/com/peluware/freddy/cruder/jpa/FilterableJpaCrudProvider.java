package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.jpa.query.EntityCountQuery;
import com.peluware.freddy.cruder.jpa.query.EntityListQuery;
import com.peluware.freddy.cruder.jpa.query.EntityExistsQuery;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor;
import com.peluware.freddy.cruder.jpa.query.EntityFindQuery;
import com.peluware.freddy.cruder.jpa.query.JpaOrder;
import com.peluware.freddy.cruder.jpa.query.JpaResult;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * JPA {@link EntityCrudProvider} that applies a {@link #predicateFilter} to every read query, for
 * cross-cutting filters such as soft-delete or multi-tenant scoping.
 *
 * <p>Search and RSQL predicates come from a {@link SearchPredicateBuilder} — by default
 * {@code omni-search-jpa} via {@link OmniSearchPredicateAdapter}; supply your own to change the
 * strategy without depending on {@code omni-search}.</p>
 *
 * @param <ENTITY> the JPA entity type
 * @param <ID>     the entity identifier type
 * @param <INPUT>  the input DTO type for create/update operations
 * @param <OUTPUT> the output DTO or projection type
 */
public abstract class FilterableJpaCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final EntityManager entityManager;
    protected final SearchPredicateBuilder searchPredicateBuilder;

    // ------------------------------------------------------------
    // CONSTRUCTORS — explicit entityClass
    // ------------------------------------------------------------

    /**
     * Creates a provider with explicit entity class, custom search predicate builder and lifecycle events.
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param entityClass            the entity class managed by this provider
     * @param events                 the CRUD lifecycle events handler
     */
    public FilterableJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
    }

    /**
     * Creates a provider with explicit entity class and custom search predicate builder,
     * using default lifecycle events (no-op).
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param entityClass            the entity class managed by this provider
     */
    public FilterableJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass) {
        this(entityManager, searchPredicateBuilder, entityClass, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider with explicit entity class, using the default {@link OmniSearchPredicateAdapter}
     * and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     * @param entityClass   the entity class managed by this provider
     */
    public FilterableJpaCrudProvider(EntityManager entityManager, Class<ENTITY> entityClass) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault(), entityClass);
    }

    // ------------------------------------------------------------
    // CONSTRUCTORS — reflection-based entityClass
    // ------------------------------------------------------------

    /**
     * Creates a provider by resolving the entity class automatically from the generic type
     * hierarchy via reflection, using a custom search predicate builder and lifecycle events.
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param events                 the CRUD lifecycle events handler
     */
    public FilterableJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
    }

    /**
     * Creates a provider by resolving the entity class automatically from the generic type
     * hierarchy via reflection, using a custom search predicate builder and default lifecycle events (no-op).
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     */
    public FilterableJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        this(entityManager, searchPredicateBuilder, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider by resolving the entity class automatically from the generic type
     * hierarchy via reflection, using the default {@link OmniSearchPredicateAdapter}
     * and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     */
    public FilterableJpaCrudProvider(EntityManager entityManager) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault());
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    /**
     * Finds an entity by its identifier using a Criteria API query.
     *
     * @throws NotFoundEntityException if no entity matches the given identifier
     */
    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityFindQuery<>(
                entityClass,
                filtered(buildIdPredicate(id)),
                () -> new NotFoundEntityException(entityClass, id)
            ).addHints(getQueryHints())
        );
    }

    /**
     * Retrieves a paginated list of entities matching the given search and query filters.
     * Uses a deferred count strategy — the total count is only resolved if needed.
     */
    @Override
    protected Page<ENTITY> internalPage(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = JpaQueryExecutor.exec(
            entityManager,
            new EntityListQuery<>(
                entityClass,
                filtered(searchPredicate(search, query)),
                sort,
                pagination
            ).addHints(getQueryHints())
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
            new EntityListQuery<>(
                entityClass,
                filtered(searchPredicate(search, query)),
                sort
            ).addHints(getQueryHints())
        );
    }

    /**
     * Every entity matching the given search and query filters — and {@link #predicateFilter} —
     * lazily, through a live JPA cursor ({@link jakarta.persistence.TypedQuery#getResultStream()})
     * — nothing is loaded until the returned stream is consumed.
     */
    @Override
    protected Stream<ENTITY> internalStream(@Nullable String search, @Nullable String query, Sort sort) {
        return JpaUtils.requireTransactionStream(entityManager, () -> JpaQueryExecutor.exec(
            entityManager,
            entityClass,
            filtered(searchPredicate(search, query)),
            JpaOrder.by(sort),
            JpaResult.<ENTITY>stream().addHints(getQueryHints())
        ));
    }

    /**
     * Counts entities matching the given search and query filters using a Criteria API count query.
     */
    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityCountQuery<>(
                entityClass,
                filtered(searchPredicate(search, query))
            ).addHints(getQueryHints())
        );
    }

    /**
     * Checks whether an entity with the given identifier exists using a Criteria API count query.
     */
    @Override
    protected boolean internalExists(ID id) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityExistsQuery<>(
                entityClass,
                filtered(buildIdPredicate(id))
            ).addHints(getQueryHints())
        );
    }

    /**
     * Persists a new entity via {@link EntityManager#persist}.
     */
    @Override
    protected ENTITY internalCreate(ENTITY entity) {
        entityManager.persist(entity);
        return entity;
    }

    /**
     * Merges an existing entity via {@link EntityManager#merge}.
     */
    @Override
    protected ENTITY internalUpdate(ENTITY entity) {
        return entityManager.merge(entity);
    }

    /**
     * Removes an entity via {@link EntityManager#remove}.
     */
    @Override
    protected void internalDelete(ENTITY entity) {
        entityManager.remove(entity);
    }

    /**
     * Executes the given function within a JPA transaction managed by the entity manager.
     */
    @Override
    protected <T> T withTransaction(Supplier<T> function) {
        return JpaUtils.requireTransaction(entityManager, function);
    }

    // ------------------------------------------------------------
    // QUERY INFRASTRUCTURE
    // ------------------------------------------------------------

    /**
     * Conjoins {@code predicate} with {@link #predicateFilter}. Use it when building custom queries
     * so they honor the same global filter.
     *
     * @param predicate the operation predicate
     * @return {@code predicate} AND {@link #predicateFilter}
     */
    protected final JpaPredicate<ENTITY> filtered(JpaPredicate<ENTITY> predicate) {
        return predicate.and(predicateFilter());
    }

    /**
     * A cross-cutting filter applied to every read query ({@code find}, {@code page}, {@code count},
     * {@code exists}) but never to writes. Override to scope every query — for soft-delete,
     * multi-tenancy, or row-level security:
     *
     * <pre>{@code (from, cb) -> cb.isFalse(from.get("deleted"))}</pre>
     *
     * @return the cross-cutting filter, or {@link JpaPredicate#all()} for none (the default)
     */
    protected JpaPredicate<ENTITY> predicateFilter() {
        return JpaPredicate.all();
    }


    /**
     * The predicate matching an entity by its identifier. Override for custom id-matching logic.
     *
     * @param id the identifier value to match
     * @return a predicate matching the identifier
     */
    protected JpaPredicate<ENTITY> buildIdPredicate(ID id) {
        return JpaPredicate.byId(entityManager.getMetamodel(), id);
    }

    /**
     * Returns JPA query hints to apply to every query executed by this provider.
     *
     * <p>
     * The default implementation returns an empty map. Subclasses may override
     * to add hints such as cache control, fetch size, or read-only optimizations
     * (e.g., {@code org.hibernate.readOnly = true}).
     * </p>
     *
     * @return a map of JPA query hints
     */
    protected Map<String, Object> getQueryHints() {
        return Map.of();
    }

    /**
     * The full-text search and RSQL predicate from the configured {@link SearchPredicateBuilder}.
     * Customize search behaviour by supplying a different builder to the constructor, not by
     * overriding; subclasses may call this to reuse it in custom queries.
     *
     * @param search normalized full-text search string, or {@code null}
     * @param query  RSQL query expression, or {@code null}
     * @return the search predicate
     */
    protected JpaPredicate<ENTITY> searchPredicate(@Nullable String search, @Nullable String query) {
        return searchPredicateBuilder.bind(entityManager.getMetamodel(), search, query);
    }

}
