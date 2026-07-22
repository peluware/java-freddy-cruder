package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.NotFoundException;
import com.peluware.freddy.cruder.OwnedCrudProvider;
import com.peluware.freddy.cruder.OwnedId;
import com.peluware.freddy.cruder.jpa.query.EntityCountQuery;
import com.peluware.freddy.cruder.jpa.query.EntityExistsQuery;
import com.peluware.freddy.cruder.jpa.query.EntityFindQuery;
import com.peluware.freddy.cruder.jpa.query.FindQuery;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor;
import com.peluware.freddy.cruder.jpa.query.JpaSelection;
import com.peluware.freddy.cruder.jpa.query.JpaSource;
import com.peluware.freddy.cruder.jpa.query.ListQuery;
import com.peluware.freddy.cruder.utils.ReflectUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Owned-resource counterpart of {@link JpaProjectedCrudProvider}: a {@link OwnedCrudProvider} scoped
 * to an owner via {@link #buildOwnerPredicate}, that reads through a two-stage pipeline —
 * {@code page}/{@code find} select {@code PROJECTION} through {@link #selection()}, then
 * {@link #mapOutput(Object, Object)} turns each row into {@code OUTPUT} — while {@code count}/
 * {@code exists} operate on the entity source directly, and writes operate on the real, managed
 * {@code ENTITY}. Same trade-off as the non-owned version — after a write, the response is built by
 * re-querying via {@link #find(Object, Object)} rather than mapping the in-memory entity directly:
 * one extra query per write, one {@link #mapOutput(Object, Object)} used everywhere.
 *
 * <p>Search and RSQL predicates come from a {@link SearchPredicateBuilder} — by default
 * {@code omni-search-jpa} via {@link OmniSearchPredicateAdapter}; supply your own to change the
 * strategy without depending on {@code omni-search}.</p>
 *
 * @param <ENTITY>     the JPA entity type
 * @param <OWNER_ID>   the identifier type of the owning (parent) resource
 * @param <ID>         the entity identifier type
 * @param <PROJECTION> the row type produced by {@link #selection()} — the entity itself, or a lean projection
 * @param <INPUT>      the input DTO type for create/update operations
 * @param <OUTPUT>     the output type returned to the consumer
 */
public abstract class JpaOwnedProjectedCrudProvider<ENTITY, OWNER_ID, ID, PROJECTION, INPUT, OUTPUT>
    implements OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> {

    protected final EntityManager entityManager;
    protected final SearchPredicateBuilder searchPredicateBuilder;
    protected final Class<ENTITY> entityClass;
    protected final Class<PROJECTION> projectionClass;
    protected final EntityCrudEvents<ENTITY, ID, INPUT> events;

    // ------------------------------------------------------------
    // CONSTRUCTORS — explicit entityClass/projectionClass
    // ------------------------------------------------------------

    /**
     * Creates a provider with explicit entity and projection classes, a custom search predicate
     * builder, and lifecycle events.
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param entityClass            the entity class this provider reads from and writes to
     * @param projectionClass        the row class produced by {@link #selection()}
     * @param events                 the CRUD lifecycle events handler
     */
    public JpaOwnedProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, Class<PROJECTION> projectionClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
        this.entityClass = entityClass;
        this.projectionClass = projectionClass;
        this.events = events;
    }

    /**
     * Creates a provider with explicit entity and projection classes and a custom search predicate
     * builder, using default lifecycle events (no-op).
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param entityClass            the entity class this provider reads from and writes to
     * @param projectionClass        the row class produced by {@link #selection()}
     */
    public JpaOwnedProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, Class<PROJECTION> projectionClass) {
        this(entityManager, searchPredicateBuilder, entityClass, projectionClass, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider with explicit entity and projection classes, using the default
     * {@link OmniSearchPredicateAdapter} and default lifecycle events (no-op).
     *
     * @param entityManager   the JPA entity manager
     * @param entityClass     the entity class this provider reads from and writes to
     * @param projectionClass the row class produced by {@link #selection()}
     */
    public JpaOwnedProjectedCrudProvider(EntityManager entityManager, Class<ENTITY> entityClass, Class<PROJECTION> projectionClass) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault(), entityClass, projectionClass);
    }

    // ------------------------------------------------------------
    // CONSTRUCTORS — reflection-based entityClass/projectionClass
    // ------------------------------------------------------------

    /**
     * Creates a provider by resolving the entity and projection classes automatically from the
     * generic type hierarchy via reflection, using a custom search predicate builder and lifecycle events.
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     * @param events                 the CRUD lifecycle events handler
     */
    @SuppressWarnings("unchecked")
    public JpaOwnedProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
        this.entityClass = (Class<ENTITY>) ReflectUtils.resolveGenericType(getClass(), JpaOwnedProjectedCrudProvider.class, 0);
        this.projectionClass = (Class<PROJECTION>) ReflectUtils.resolveGenericType(getClass(), JpaOwnedProjectedCrudProvider.class, 3);
        this.events = events;
    }

    /**
     * Creates a provider by resolving the entity and projection classes automatically from the
     * generic type hierarchy via reflection, using a custom search predicate builder and default
     * lifecycle events (no-op).
     *
     * @param entityManager          the JPA entity manager
     * @param searchPredicateBuilder the predicate builder used for search and RSQL filtering
     */
    public JpaOwnedProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        this(entityManager, searchPredicateBuilder, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider by resolving the entity and projection classes automatically from the
     * generic type hierarchy via reflection, using the default {@link OmniSearchPredicateAdapter}
     * and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     */
    public JpaOwnedProjectedCrudProvider(EntityManager entityManager) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault());
    }

    // ------------------------------------------------------------
    // READ OPERATIONS
    // ------------------------------------------------------------

    @Override
    public Page<OUTPUT> page(@NotNull OWNER_ID ownerId, @Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = JpaQueryExecutor.exec(
            entityManager,
            new ListQuery<>(
                projectionClass,
                JpaSource.root(entityClass),
                selection(),
                filtered(ownerPredicate(ownerId).and(searchPredicate(search, query))),
                sort,
                pagination
            ).addHints(getQueryHints())
        );
        var mapped = content.stream().map(projection -> mapOutput(ownerId, projection)).toList();
        return Page.deferred(mapped, pagination, sort, () -> count(ownerId, search, query));
    }

    @Override
    public OUTPUT find(@NotNull OWNER_ID ownerId, @NotNull ID id) throws NotFoundEntityException {
        var projection = JpaQueryExecutor.exec(
            entityManager,
            new FindQuery<>(
                projectionClass,
                JpaSource.root(entityClass),
                selection(),
                filtered(ownerPredicate(ownerId).and(buildIdPredicate(id))),
                () -> new NotFoundEntityException(entityClass, new OwnedId<>(ownerId, id))
            ).addHints(getQueryHints())
        );
        return mapOutput(ownerId, projection);
    }

    @Override
    public long count(@NotNull OWNER_ID ownerId, @Nullable String search, @Nullable String query) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityCountQuery<>(
                entityClass,
                filtered(ownerPredicate(ownerId).and(searchPredicate(search, query)))
            ).addHints(getQueryHints())
        );
    }

    @Override
    public boolean exists(@NotNull OWNER_ID ownerId, @NotNull ID id) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityExistsQuery<>(
                entityClass,
                filtered(ownerPredicate(ownerId).and(buildIdPredicate(id)))
            ).addHints(getQueryHints())
        );
    }

    // ------------------------------------------------------------
    // WRITE OPERATIONS
    // ------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Persists a new entity mapped from {@code input}, then builds the response by calling
     * {@link #find(Object, Object)} on the owner and the entity's generated identifier — the same
     * projection query and {@link #mapOutput(Object, Object)} used by every read.</p>
     */
    @Override
    public OUTPUT create(@NotNull OWNER_ID ownerId, @NotNull @Valid INPUT input) throws NotFoundException {
        return withTransaction(() -> {
            var entity = newEntity();

            mapInput(ownerId, input, entity, true);
            events.onBeforeCreate(input, entity);

            var created = internalCreate(ownerId, entity);
            afterCreate(ownerId, input, created);

            events.onAfterCreate(input, created);
            events.eachEntity(created);

            return find(ownerId, idOf(created));
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the entity for mutation via {@link #loadForMutation(Object, Object)}, applies
     * {@code input}, then builds the response via {@link #find(Object, Object)} rather than mapping
     * the in-memory entity.</p>
     */
    @Override
    public OUTPUT update(@NotNull OWNER_ID ownerId, @NotNull ID id, @NotNull @Valid INPUT input) throws NotFoundException {
        return withTransaction(() -> {
            var entity = loadForMutation(ownerId, id);

            mapInput(ownerId, input, entity, false);
            events.onBeforeUpdate(input, entity);

            var updated = internalUpdate(ownerId, entity);
            afterUpdate(ownerId, input, updated);

            events.onAfterUpdate(input, updated);
            events.eachEntity(updated);

            return find(ownerId, id);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(@NotNull OWNER_ID ownerId, @NotNull ID id) throws NotFoundException {
        withTransaction(() -> {
            var entity = loadForMutation(ownerId, id);

            events.onBeforeDelete(entity);
            internalDelete(ownerId, entity);
            events.onAfterDelete(entity);

            return Void.class;
        });
    }

    // ------------------------------------------------------------
    // PROJECTION / MAPPING CONTRACT
    // ------------------------------------------------------------

    /**
     * Builds the {@code SELECT} clause producing {@code PROJECTION} from the entity source —
     * either a lean {@code cb.construct(...)}, or {@link JpaSelection#self()} to select the whole
     * entity when {@link #mapOutput(Object, Object)} needs real Java-side mapping:
     *
     * <pre>{@code
     * protected JpaSelection<CatalogThreat, CatalogThreatRef> selection() {
     *     return (from, cb) -> cb.construct(CatalogThreatRef.class, from.get("id"), from.get("threat").get("name"));
     * }
     * }</pre>
     *
     * @return the projection selection used by {@code page} and {@code find}
     */
    protected abstract JpaSelection<ENTITY, PROJECTION> selection();

    /**
     * Maps a {@code PROJECTION} row to the {@code OUTPUT} returned to the consumer, given the
     * owner it was read under. If {@code PROJECTION} already equals {@code OUTPUT} (the
     * lean-projection case), this ignores {@code ownerId} and returns {@code projection} as-is.
     *
     * @param ownerId    the identifier of the owning resource the row was read under
     * @param projection the row produced by {@link #selection()}
     * @return the mapped output
     */
    protected abstract OUTPUT mapOutput(OWNER_ID ownerId, PROJECTION projection);

    /**
     * Maps the contents of the input DTO into the given entity instance.
     *
     * @param ownerId the owner scope of the operation
     * @param input   the input DTO
     * @param entity  the entity to populate
     * @param isNew   whether this is a creation (true) or update (false)
     */
    protected abstract void mapInput(OWNER_ID ownerId, INPUT input, ENTITY entity, boolean isNew);

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
     * A cross-cutting filter applied to every query, independent of the owner scope enforced by
     * {@link #buildOwnerPredicate}. Override to scope every query further — for soft-delete or
     * row-level security:
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
     * This method is called for every operation and must enforce the ownership
     * boundary at the database level. Common implementations:
     * </p>
     *
     * <ul>
     *   <li>Foreign key: {@code cb.equal(from.get("catalog").get("id"), ownerId)}</li>
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
     * The predicate matching an entity by its identifier. Override for custom id-matching logic.
     *
     * @param id the identifier value to match
     * @return a predicate matching the identifier
     */
    protected JpaPredicate<ENTITY> buildIdPredicate(ID id) {
        return JpaPredicate.byId(entityManager.getMetamodel(), id);
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
     * Returns JPA query hints to apply to every query executed by this provider.
     *
     * <p>
     * The default implementation returns an empty map. Subclasses may override
     * to add hints such as cache control, fetch size, or query timeouts.
     * </p>
     *
     * @return a map of JPA query hints
     */
    protected Map<String, Object> getQueryHints() {
        return Map.of();
    }

    // ------------------------------------------------------------
    // PERSISTENCE / MUTATION-LOAD
    // ------------------------------------------------------------

    /**
     * Loads the real, managed entity to mutate for {@code update}/{@code delete}, scoped to the
     * owner — deliberately distinct from {@link #selection()}, which is only for {@code page}/
     * {@code find}. Honors the same {@link #predicateFilter()} and {@link #getQueryHints()} as reads.
     *
     * @param ownerId the identifier of the owning resource
     * @param id      the identifier of the entity to load
     * @return the managed entity
     * @throws NotFoundEntityException if no entity matches both identifiers
     */
    protected ENTITY loadForMutation(OWNER_ID ownerId, ID id) throws NotFoundEntityException {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityFindQuery<>(entityClass, filtered(ownerPredicate(ownerId).and(buildIdPredicate(id))), () -> new NotFoundEntityException(entityClass, new OwnedId<>(ownerId, id))).addHints(getQueryHints())
        );
    }

    /**
     * Creates a new instance of the managed entity type using its default constructor.
     *
     * <p>Subclasses may override when entities require factory methods instead of reflection.</p>
     *
     * @return a new entity instance
     */
    protected ENTITY newEntity() {
        try {
            return entityClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate entity: " + entityClass.getName(), e);
        }
    }

    /**
     * Resolves the identifier of a just-persisted entity, via the persistence provider's own
     * metadata — no {@code getId()} contract required on {@code ENTITY}.
     *
     * @param entity the freshly persisted entity
     * @return the entity's identifier
     */
    @SuppressWarnings("unchecked")
    protected ID idOf(ENTITY entity) {
        return (ID) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
    }

    /**
     * Persists a new entity via {@link EntityManager#persist}.
     *
     * @param ownerId the owner scope of the operation
     * @param entity  the entity to persist
     */
    protected ENTITY internalCreate(OWNER_ID ownerId, ENTITY entity) {
        entityManager.persist(entity);
        return entity;
    }

    /**
     * Merges an existing entity via {@link EntityManager#merge}.
     *
     * @param ownerId the owner scope of the operation
     * @param entity  the entity to merge
     */
    protected ENTITY internalUpdate(OWNER_ID ownerId, ENTITY entity) {
        return entityManager.merge(entity);
    }

    /**
     * Removes an entity via {@link EntityManager#remove}.
     *
     * @param ownerId the owner scope of the operation
     * @param entity  the entity to remove
     */
    protected void internalDelete(OWNER_ID ownerId, ENTITY entity) {
        entityManager.remove(entity);
    }

    /**
     * Executes the given function within a JPA transaction managed by the entity manager.
     */
    protected <T> T withTransaction(Supplier<T> function) {
        return JpaUtils.requireTransaction(entityManager, function);
    }

    // ------------------------------------------------------------
    // EXTENSION HOOKS
    // ------------------------------------------------------------

    /**
     * Hook executed after {@link #internalCreate(Object)}, inside the same transaction, with the
     * owner scope, the input DTO and the persisted entity (its generated identifier is available).
     *
     * <p>Override to persist dependent entities that need the parent's identifier. This is provider
     * business logic, distinct from the cross-cutting {@link EntityCrudEvents#onAfterCreate}.</p>
     *
     * @param ownerId the owner scope of the operation
     * @param input   the input DTO of the create operation
     * @param created the freshly persisted entity
     */
    protected void afterCreate(OWNER_ID ownerId, INPUT input, ENTITY created) {
        // Subclasses may override
    }

    /**
     * Hook executed after {@link #internalUpdate(Object)}, inside the same transaction, with the
     * owner scope, the input DTO and the persisted entity.
     *
     * <p>Override to reconcile dependent entities from the input DTO after the parent is updated.
     * This is provider business logic, distinct from the cross-cutting
     * {@link EntityCrudEvents#onAfterUpdate}.</p>
     *
     * @param ownerId the owner scope of the operation
     * @param input   the input DTO of the update operation
     * @param updated the freshly persisted entity
     */
    protected void afterUpdate(OWNER_ID ownerId, INPUT input, ENTITY updated) {
        // Subclasses may override
    }
}
