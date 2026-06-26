package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.OwnedEntityCrudProvider;
import com.peluware.freddy.cruder.OwnedId;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * JPA-specific implementation of {@link OwnedEntityCrudProvider} that builds all queries
 * directly via the JPA Criteria API, delegating predicate construction to a
 * {@link SearchPredicateBuilder}.
 *
 * <p>
 * This is the owned-resource counterpart of {@link FilterableJpaCrudProvider}. Every
 * query is automatically scoped to the given owner by combining the operation predicate
 * with the result of {@link #buildOwnerPredicate}, which subclasses must implement to
 * enforce the ownership constraint (e.g., a foreign-key equality or a tenant column).
 * </p>
 *
 * <p>
 * All queries are executed through {@link #runQuery}, which passes the combined predicate
 * through {@link #predicateFilter} before execution — a secondary hook for cross-cutting
 * concerns such as soft-delete filters or row-level security.
 * </p>
 *
 * <p>
 * The default {@link SearchPredicateBuilder} delegates to {@code omni-search-jpa} via
 * {@link OmniSearchPredicateAdapter}. Provide a custom implementation to use a
 * different search strategy without any dependency on {@code omni-search}.
 * </p>
 *
 * @param <ENTITY>   the JPA entity type
 * @param <OWNER_ID> the identifier type of the owning (parent) resource
 * @param <ID>       the entity identifier type
 * @param <INPUT>    the input DTO type for create/update operations
 * @param <OUTPUT>   the output DTO or projection type
 */
public abstract class FilterableOwnedJpaCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> extends OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> {

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
    public FilterableOwnedJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
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
    public FilterableOwnedJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass) {
        this(entityManager, searchPredicateBuilder, entityClass, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider with explicit entity class, using the default {@link OmniSearchPredicateAdapter}
     * and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     * @param entityClass   the entity class managed by this provider
     */
    public FilterableOwnedJpaCrudProvider(EntityManager entityManager, Class<ENTITY> entityClass) {
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
    public FilterableOwnedJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
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
    public FilterableOwnedJpaCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        this(entityManager, searchPredicateBuilder, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider by resolving the entity class automatically from the generic type
     * hierarchy via reflection, using the default {@link OmniSearchPredicateAdapter}
     * and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     */
    public FilterableOwnedJpaCrudProvider(EntityManager entityManager) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault());
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    /**
     * Finds an entity by owner and identifier using a Criteria API query.
     *
     * @throws NotFoundEntityException if no entity matches both identifiers
     */
    @Override
    protected ENTITY internalFind(OWNER_ID ownerId, ID id) throws NotFoundEntityException {
        return runQuery(
            entityClass,
            (root, cb) -> cb.and(buildOwnerPredicate(root, cb, ownerId), buildIdPredicate(root, cb, id)),
            JpaCriteriaExecutor.first()
        ).orElseThrow(() -> new NotFoundEntityException(entityClass, new OwnedId<>(ownerId, id)));
    }

    /**
     * Retrieves a paginated list of entities belonging to the given owner, matching
     * the given search and query filters. Uses a deferred count strategy.
     */
    @Override
    protected Page<ENTITY> internalPage(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = runQuery(
            entityClass,
            (root, cb) -> cb.and(buildOwnerPredicate(root, cb, ownerId), buildSearchPredicate(root, cb, search, query)),
            JpaCriteriaExecutor.list(sort, pagination)
        );
        return Page.deferred(
            content,
            pagination,
            sort,
            () -> internalCount(ownerId, search, query)
        );
    }

    /**
     * Counts entities belonging to the given owner matching the given search and query filters.
     */
    @Override
    protected long internalCount(OWNER_ID ownerId, @Nullable String search, @Nullable String query) {
        return runQuery(
            Long.class,
            (root, cb) -> cb.and(buildOwnerPredicate(root, cb, ownerId), buildSearchPredicate(root, cb, search, query)),
            JpaCriteriaExecutor.count()
        );
    }

    /**
     * Checks whether an entity with the given identifier exists within the owner's scope.
     */
    @Override
    protected boolean internalExists(OWNER_ID ownerId, ID id) {
        return runQuery(
            Long.class,
            (root, cb) -> cb.and(buildOwnerPredicate(root, cb, ownerId), buildIdPredicate(root, cb, id)),
            JpaCriteriaExecutor.exists()
        );
    }

    /**
     * Persists a new entity via {@link EntityManager#persist}.
     */
    @Override
    protected ENTITY internalCreate(OWNER_ID ownerId, ENTITY entity) {
        entityManager.persist(entity);
        return entity;
    }

    /**
     * Merges an existing entity via {@link EntityManager#merge}.
     */
    @Override
    protected ENTITY internalUpdate(OWNER_ID ownerId, ENTITY entity) {
        return entityManager.merge(entity);
    }

    /**
     * Removes an entity via {@link EntityManager#remove}.
     */
    @Override
    protected void internalDelete(OWNER_ID ownerId, ENTITY entity) {
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
     * Builds and executes a Criteria API query against the entity table using the default
     * query hints from {@link #getQueryHints()}.
     *
     * <p>
     * The predicate produced by {@code predicateLoader} is passed through
     * {@link #predicateFilter} before being applied to the query, allowing
     * subclasses to inject global filters transparently.
     * </p>
     *
     * @param resultType      the type of the query result (entity class or {@code Long} for counts)
     * @param predicateLoader function that builds the main predicate given a {@link Root} and {@link CriteriaBuilder}
     * @param executor        strategy that configures and executes the query
     * @param <T>             the result row type
     * @param <R>             the final return type
     * @return the query result
     */
    protected final <T, R> R runQuery(Class<T> resultType, BiFunction<Root<ENTITY>, CriteriaBuilder, Predicate> predicateLoader, JpaCriteriaExecutor<ENTITY, T, R> executor) {
        return runQuery(resultType, predicateLoader, executor, getQueryHints());
    }

    /**
     * Builds and executes a Criteria API query against the entity table using explicit query hints,
     * overriding the defaults from {@link #getQueryHints()}.
     *
     * <p>
     * The predicate produced by {@code predicateLoader} is passed through
     * {@link #predicateFilter} before being applied to the query, allowing
     * subclasses to inject global filters transparently.
     * </p>
     *
     * @param resultType      the type of the query result (entity class or {@code Long} for counts)
     * @param predicateLoader function that builds the main predicate given a {@link Root} and {@link CriteriaBuilder}
     * @param executor        strategy that configures and executes the query
     * @param hints           JPA query hints to apply, overriding {@link #getQueryHints()}
     * @param <T>             the result row type
     * @param <R>             the final return type
     * @return the query result
     */
    protected final <T, R> R runQuery(Class<T> resultType, BiFunction<Root<ENTITY>, CriteriaBuilder, Predicate> predicateLoader, JpaCriteriaExecutor<ENTITY, T, R> executor, Map<String, Object> hints) {
        return JpaQueryHelpers.query(
            entityManager,
            entityClass,
            resultType,
            (root, cb) -> predicateFilter(root, cb, predicateLoader.apply(root, cb)),
            executor,
            hints
        );
    }

    /**
     * Applies global predicate filters to every read query before execution.
     *
     * <p>
     * This hook is called for all read operations ({@code find}, {@code page},
     * {@code count}, {@code exists}) but <strong>not</strong> for write operations
     * ({@code create}, {@code update}, {@code delete}), which bypass the Criteria API.
     * </p>
     *
     * <p>
     * The default implementation returns the original predicate unchanged.
     * Subclasses may override to inject cross-cutting predicates that must apply to
     * every query, independently of the owner scope already enforced by
     * {@link #buildOwnerPredicate}. Examples:
     * </p>
     *
     * <ul>
     *   <li>Soft-delete filter: {@code cb.and(original, cb.isFalse(root.get("deleted")))}</li>
     *   <li>Row-level security constraints</li>
     * </ul>
     *
     * @param root              the query root representing the entity being queried
     * @param cb                the criteria builder, available for combining predicates
     * @param originalPredicate the predicate built by the operation (already includes the owner constraint)
     * @return the final predicate to apply, potentially combined with additional conditions
     */
    protected Predicate predicateFilter(Root<ENTITY> root, CriteriaBuilder cb, Predicate originalPredicate) {
        return originalPredicate;
    }

    /**
     * Builds a predicate that restricts queries to entities belonging to the given owner.
     *
     * <p>
     * This method is called for every read operation and must enforce the ownership
     * boundary at the database level. Common implementations:
     * </p>
     *
     * <ul>
     *   <li>Foreign key: {@code cb.equal(root.get("userId"), ownerId)}</li>
     *   <li>Tenant column: {@code cb.equal(root.get("tenantId"), ownerId)}</li>
     *   <li>Composite key join: {@code cb.equal(root.get("order").get("id"), ownerId)}</li>
     * </ul>
     *
     * @param root    the query root
     * @param cb      the criteria builder
     * @param ownerId the identifier of the owning resource
     * @return a predicate that scopes the query to the given owner
     */
    protected abstract Predicate buildOwnerPredicate(Root<ENTITY> root, CriteriaBuilder cb, OWNER_ID ownerId);

    /**
     * Builds a predicate for full-text search and RSQL filtering using the configured
     * {@link SearchPredicateBuilder}.
     *
     * @param root   the query root
     * @param cb     the criteria builder
     * @param search normalized full-text search string, or {@code null}
     * @param query  RSQL query expression, or {@code null}
     * @return the resulting predicate
     */
    protected Predicate buildSearchPredicate(Root<ENTITY> root, CriteriaBuilder cb, @Nullable String search, @Nullable String query) {
        return searchPredicateBuilder.build(root, cb, entityManager.getMetamodel(), search, query);
    }

    /**
     * Builds an equality predicate matching the entity's identifier field against the given {@code id}.
     *
     * @param root the query root
     * @param cb   the criteria builder
     * @param id   the identifier value to match
     * @return an equality predicate on the ID field
     */
    protected Predicate buildIdPredicate(Root<ENTITY> root, CriteriaBuilder cb, ID id) {
        var idFieldName = JpaUtils.getIdFieldName(entityManager.getMetamodel(), entityClass);
        return cb.equal(root.get(idFieldName), id);
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
}
