package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.CrudProvider;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.jpa.query.EntityCountQuery;
import com.peluware.freddy.cruder.jpa.query.EntityExistsQuery;
import com.peluware.freddy.cruder.jpa.query.EntityFindQuery;
import com.peluware.freddy.cruder.jpa.query.FindQuery;
import com.peluware.freddy.cruder.jpa.query.JpaGroupBy;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor;
import com.peluware.freddy.cruder.jpa.query.JpaSelection;
import com.peluware.freddy.cruder.jpa.query.JpaSource;
import com.peluware.freddy.cruder.jpa.query.ListQuery;
import com.peluware.freddy.cruder.utils.ReflectUtils;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link CrudProvider} that reads through a two-stage pipeline — {@code page}/{@code find} select
 * {@code PROJECTION} through {@link #selection()}, then {@link #mapOutput(Object)} turns each row
 * into {@code OUTPUT} — while {@code count}/{@code exists} operate on the entity source directly,
 * and writes ({@code create}/{@code update}/{@code delete}) operate on the real, managed
 * {@code ENTITY}, since the projection is only ever cheaper for reads.
 *
 * <p>{@code PROJECTION} is deliberately free to be anything selectable, not just a lean DTO:</p>
 * <ul>
 *   <li><b>A lean projection</b> — {@code selection()} does {@code cb.construct(...)} into a small
 *   DTO that already matches {@code OUTPUT}; {@link #mapOutput(Object)} is then the identity. Avoids
 *   loading columns/associations a caller never needs, e.g. one with {@code @Formula} columns.</li>
 *   <li><b>The entity itself</b> — {@code selection()} returns {@link JpaSelection#self()} and
 *   {@link #mapOutput(Object)} does real Java-side mapping (nested objects, conditional nulls).</li>
 * </ul>
 *
 * <p>After {@code create}/{@code update} persist or merge the entity, the response is built by
 * re-querying via {@link #find(Object)} rather than mapping the in-memory entity directly — one
 * extra query per write, in exchange for a single {@link #mapOutput(Object)} used consistently by
 * every operation, read or write. Listing/paging is unaffected: it still costs exactly one query.</p>
 *
 * <p>Search and RSQL predicates come from a {@link SearchPredicateBuilder} — by default
 * {@code omni-search-jpa} via {@link OmniSearchPredicateAdapter}; supply your own to change the
 * strategy without depending on {@code omni-search}.</p>
 *
 * @param <ENTITY>     the JPA entity type
 * @param <ID>         the entity identifier type
 * @param <PROJECTION> the row type produced by {@link #selection()} — the entity itself, or a lean projection
 * @param <INPUT>      the input DTO type for create/update operations
 * @param <OUTPUT>     the output type returned to the consumer
 */
public abstract class JpaProjectedCrudProvider<ENTITY, ID, PROJECTION, INPUT, OUTPUT> implements CrudProvider<ID, INPUT, OUTPUT> {

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
    public JpaProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, Class<PROJECTION> projectionClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
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
    public JpaProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, Class<ENTITY> entityClass, Class<PROJECTION> projectionClass) {
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
    public JpaProjectedCrudProvider(EntityManager entityManager, Class<ENTITY> entityClass, Class<PROJECTION> projectionClass) {
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
    public JpaProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
        this.entityClass = (Class<ENTITY>) ReflectUtils.resolveGenericType(getClass(), JpaProjectedCrudProvider.class, 0);
        this.projectionClass = (Class<PROJECTION>) ReflectUtils.resolveGenericType(getClass(), JpaProjectedCrudProvider.class, 2);
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
    public JpaProjectedCrudProvider(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        this(entityManager, searchPredicateBuilder, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider by resolving the entity and projection classes automatically from the
     * generic type hierarchy via reflection, using the default {@link OmniSearchPredicateAdapter}
     * and default lifecycle events (no-op).
     *
     * @param entityManager the JPA entity manager
     */
    public JpaProjectedCrudProvider(EntityManager entityManager) {
        this(entityManager, OmniSearchPredicateAdapter.ofDefault());
    }

    // ------------------------------------------------------------
    // READ OPERATIONS
    // ------------------------------------------------------------

    @Override
    public Page<OUTPUT> page(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = JpaQueryExecutor.exec(
            entityManager,
            new ListQuery<>(
                projectionClass,
                JpaSource.root(entityClass),
                selection(),
                filtered(searchPredicate(search, query)),
                groupBy(),
                sort,
                pagination
            ).addHints(getQueryHints())
        );
        var mapped = content.stream().map(this::mapOutput).toList();
        return Page.deferred(mapped, pagination, sort, () -> count(search, query));
    }

    @Override
    public OUTPUT find(@NotNull ID id) throws NotFoundEntityException {
        var projection = JpaQueryExecutor.exec(
            entityManager,
            new FindQuery<>(
                projectionClass,
                JpaSource.root(entityClass),
                selection(),
                filtered(buildIdPredicate(id)),
                groupBy(),
                () -> new NotFoundEntityException(entityClass, id)
            ).addHints(getQueryHints())
        );
        return mapOutput(projection);
    }

    @Override
    public long count(@Nullable String search, @Nullable String query) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityCountQuery<>(
                entityClass,
                filtered(searchPredicate(search, query))
            ).addHints(getQueryHints())
        );
    }

    @Override
    public boolean exists(@NotNull ID id) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityExistsQuery<>(
                entityClass,
                filtered(buildIdPredicate(id))
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
     * {@link #find(Object)} on the entity's generated identifier — the same projection query and
     * {@link #mapOutput(Object)} used by every read.</p>
     */
    @Override
    public OUTPUT create(@NotNull @Valid INPUT input) {
        return withTransaction(() -> {
            var entity = newEntity();

            mapInput(input, entity, true);
            events.onBeforeCreate(input, entity);

            var created = internalCreate(entity);
            afterCreate(input, created);

            events.onAfterCreate(input, created);
            events.eachEntity(created);

            return find(idOf(created));
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the entity for mutation via {@link #loadForMutation(Object)}, applies {@code input},
     * then builds the response via {@link #find(Object)} rather than mapping the in-memory entity.</p>
     */
    @Override
    public OUTPUT update(@NotNull ID id, @NotNull @Valid INPUT input) throws NotFoundEntityException {
        return withTransaction(() -> {
            var entity = loadForMutation(id);

            mapInput(input, entity, false);
            events.onBeforeUpdate(input, entity);

            var updated = internalUpdate(entity);
            afterUpdate(input, updated);

            events.onAfterUpdate(input, updated);
            events.eachEntity(updated);

            return find(id);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(@NotNull ID id) throws NotFoundEntityException {
        withTransaction(() -> {
            var entity = loadForMutation(id);

            events.onBeforeDelete(entity);
            internalDelete(entity);
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
     * entity when {@link #mapOutput(Object)} needs real Java-side mapping:
     *
     * <pre>{@code
     * protected JpaSelection<AssetType, AssetTypeRef> selection() {
     *     return (from, cb) -> cb.construct(AssetTypeRef.class, from.get("id"), from.get("name"));
     * }
     * }</pre>
     *
     * @return the projection selection used by {@code page} and {@code find}
     */
    protected abstract JpaSelection<ENTITY, PROJECTION> selection();

    /**
     * Maps a {@code PROJECTION} row to the {@code OUTPUT} returned to the consumer. If
     * {@code PROJECTION} already equals {@code OUTPUT} (the lean-projection case), this is the
     * identity.
     *
     * @param projection the row produced by {@link #selection()}
     * @return the mapped output
     */
    protected abstract OUTPUT mapOutput(PROJECTION projection);

    /**
     * Maps the contents of the input DTO into the given entity instance.
     *
     * @param input  the input DTO
     * @param entity the entity to populate
     * @param isNew  whether this is a creation (true) or update (false)
     */
    protected abstract void mapInput(INPUT input, ENTITY entity, boolean isNew);

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
     * A cross-cutting filter applied to every query ({@code find}, {@code page}, {@code count},
     * {@code exists}). Override to scope every query — for soft-delete, multi-tenancy, or row-level
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
     * The {@code GROUP BY} clause applied to {@code page}/{@code find}, for projections that
     * aggregate over a plural association (e.g. summing a child collection's field) alongside the
     * entity's own columns. Override with {@link JpaGroupBy#self()} to group by the entity root —
     * grouping by the whole entity, not just its id, lets the projection select any of its other
     * columns freely:
     *
     * <pre>{@code JpaGroupBy.self()}</pre>
     *
     * @return the grouping expressions, or {@link JpaGroupBy#none()} for none (the default)
     */
    protected JpaGroupBy<ENTITY> groupBy() {
        return JpaGroupBy.none();
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
    protected JpaPredicate<ENTITY> searchPredicate(@Nullable String search, @Nullable String query) {
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
     * Loads the real, managed entity to mutate for {@code update}/{@code delete} — deliberately
     * distinct from {@link #selection()}, which is only for {@code page}/{@code find}. Honors the
     * same {@link #predicateFilter()} and {@link #getQueryHints()} as reads.
     *
     * @param id the identifier of the entity to load
     * @return the managed entity
     * @throws NotFoundEntityException if no entity matches the given identifier
     */
    protected ENTITY loadForMutation(ID id) throws NotFoundEntityException {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityFindQuery<>(entityClass, filtered(buildIdPredicate(id)), () -> new NotFoundEntityException(entityClass, id)).addHints(getQueryHints())
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
     */
    protected ENTITY internalCreate(ENTITY entity) {
        entityManager.persist(entity);
        return entity;
    }

    /**
     * Merges an existing entity via {@link EntityManager#merge}.
     */
    protected ENTITY internalUpdate(ENTITY entity) {
        return entityManager.merge(entity);
    }

    /**
     * Removes an entity via {@link EntityManager#remove}.
     */
    protected void internalDelete(ENTITY entity) {
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
     * input DTO and the persisted entity (its generated identifier is available).
     *
     * <p>Override to persist dependent entities that need the parent's identifier. This is provider
     * business logic, distinct from the cross-cutting {@link EntityCrudEvents#onAfterCreate}.</p>
     *
     * @param input   the input DTO of the create operation
     * @param created the freshly persisted entity
     */
    protected void afterCreate(INPUT input, ENTITY created) {
        // Subclasses may override
    }

    /**
     * Hook executed after {@link #internalUpdate(Object)}, inside the same transaction, with the
     * input DTO and the persisted entity.
     *
     * <p>Override to reconcile dependent entities from the input DTO after the parent is updated.
     * This is provider business logic, distinct from the cross-cutting
     * {@link EntityCrudEvents#onAfterUpdate}.</p>
     *
     * @param input   the input DTO of the update operation
     * @param updated the freshly persisted entity
     */
    protected void afterUpdate(INPUT input, ENTITY updated) {
        // Subclasses may override
    }
}
