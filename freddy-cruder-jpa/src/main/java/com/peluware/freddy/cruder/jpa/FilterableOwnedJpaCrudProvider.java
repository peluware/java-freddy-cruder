package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.OwnedEntityCrudProvider;
import com.peluware.freddy.cruder.OwnedId;
import com.peluware.freddy.cruder.jpa.query.EntityCountQuery;
import com.peluware.freddy.cruder.jpa.query.EntityListQuery;
import com.peluware.freddy.cruder.jpa.query.EntityExistsQuery;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor;
import com.peluware.freddy.cruder.jpa.query.EntityFindQuery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Owned-resource counterpart of {@link FilterableJpaCrudProvider}: every read query is scoped to
 * an owner via {@link #buildOwnerPredicate} (which subclasses implement — e.g. a foreign-key or
 * tenant-column equality), and further filtered by the {@link #predicateFilter} hook for
 * cross-cutting concerns such as soft-delete or row-level security.
 *
 * <p>Search and RSQL predicates come from a {@link SearchPredicateBuilder} — by default
 * {@code omni-search-jpa} via {@link OmniSearchPredicateAdapter}; supply your own to change the
 * strategy without depending on {@code omni-search}.</p>
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
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityFindQuery<>(entityClass, filtered(ownerPredicate(ownerId).and(buildIdPredicate(id))), () -> new NotFoundEntityException(entityClass, new OwnedId<>(ownerId, id))).addHints(getQueryHints())
        );
    }

    /**
     * Retrieves a paginated list of entities belonging to the given owner, matching
     * the given search and query filters. Uses a deferred count strategy.
     */
    @Override
    protected Page<ENTITY> internalPage(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = JpaQueryExecutor.exec(
            entityManager,
            new EntityListQuery<>(
                entityClass,
                filtered(ownerPredicate(ownerId).and(searchPredicate(search, query))),
                sort,
                pagination
            ).addHints(getQueryHints())
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
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityCountQuery<>(
                entityClass,
                filtered(ownerPredicate(ownerId).and(searchPredicate(search, query)))).addHints(getQueryHints()
            )
        );
    }

    /**
     * Checks whether an entity with the given identifier exists within the owner's scope.
     */
    @Override
    protected boolean internalExists(OWNER_ID ownerId, ID id) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityExistsQuery<>(
                entityClass,
                filtered(ownerPredicate(ownerId).and(buildIdPredicate(id)))).addHints(getQueryHints()
            )
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
     * Conjoins {@code predicate} with {@link #predicateFilter}. Use it when building custom queries
     * so they honor the same global filter.
     *
     * @param predicate the operation predicate (already including the owner constraint)
     * @return {@code predicate} AND {@link #predicateFilter}
     */
    protected final JpaPredicate<ENTITY> filtered(JpaPredicate<ENTITY> predicate) {
        return predicate.and(predicateFilter());
    }

    /**
     * A cross-cutting filter applied to every read query ({@code find}, {@code page}, {@code count},
     * {@code exists}) but never to writes, independent of the owner scope enforced by
     * {@link #buildOwnerPredicate}. Override to scope every query — for soft-delete or row-level
     * security:
     *
     * <pre>{@code (from, cb) -> cb.isFalse(from.get("deleted"))}</pre>
     *
     * @return the cross-cutting filter, or {@link JpaPredicate#all()} for none (the default)
     */
    protected JpaPredicate<ENTITY> predicateFilter() {
        return JpaPredicate.all();
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
     *   <li>Foreign key: {@code cb.equal(from.get("userId"), ownerId)}</li>
     *   <li>Tenant column: {@code cb.equal(from.get("tenantId"), ownerId)}</li>
     *   <li>Composite key join: {@code cb.equal(from.get("order").get("id"), ownerId)}</li>
     * </ul>
     *
     * @param from    the query source (root or join)
     * @param cb      the criteria builder
     * @param ownerId the identifier of the owning resource
     * @return a predicate that scopes the query to the given owner
     */
    protected abstract Predicate buildOwnerPredicate(From<?, ENTITY> from, CriteriaBuilder cb, OWNER_ID ownerId);

    /**
     * The owner-scoping predicate as a {@link JpaPredicate}, for composing with other predicates
     * (via {@link JpaPredicate#and}) when building custom owned queries.
     *
     * @param ownerId the identifier of the owning resource
     * @return {@link #buildOwnerPredicate} as a predicate
     */
    protected final JpaPredicate<ENTITY> ownerPredicate(OWNER_ID ownerId) {
        return (from, cb) -> buildOwnerPredicate(from, cb, ownerId);
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
    protected final JpaPredicate<ENTITY> searchPredicate(@Nullable String search, @Nullable String query) {
        return searchPredicateBuilder.bind(entityManager.getMetamodel(), search, query);
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
}
