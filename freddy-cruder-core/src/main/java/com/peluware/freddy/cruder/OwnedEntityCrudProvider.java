package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.utils.StringUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A high-level, framework-agnostic CRUD provider for entities that belong to a parent
 * (owner) resource, encapsulating the full lifecycle of owned entity persistence operations
 * while delegating the low-level storage logic to subclasses.
 *
 * <p>
 * This abstraction mirrors {@link EntityCrudProvider} in structure and philosophy, extending
 * it with an {@code OWNER_ID} context that is threaded through every operation. All
 * {@code internal*} methods receive the owner identifier so that subclasses can enforce
 * the ownership scope at the persistence layer (e.g. filtering by a foreign key, a tenant
 * column, or a composite key).
 * </p>
 *
 * <ul>
 *   <li>Standardized owned CRUD operations defined by {@link OwnedCrudProvider}</li>
 *   <li>Entity/DTO mapping hooks via {@link #mapInput(INPUT, ENTITY, boolean, CrudOptions)} and {@link #mapOutput(ENTITY, CrudOptions)}</li>
 *   <li>Automatic invocation of CRUD lifecycle event callbacks via {@link CrudEvents}</li>
 *   <li>Pre- and post-operation hooks for cross-cutting concerns</li>
 *   <li>Optional transaction wrapping through {@link #withTransaction(Supplier)}</li>
 * </ul>
 *
 * <p>
 * Concrete implementations must supply the underlying persistence logic by implementing
 * the {@code internal*} methods such as {@link #internalFind(OWNER_ID, ID, CrudOptions)},
 * {@link #internalCreate(OWNER_ID, ENTITY, CrudOptions)}, and others.
 * </p>
 *
 * @param <ENTITY>   the domain entity type managed by this provider
 * @param <OWNER_ID> the identifier type of the owning (parent) resource
 * @param <ID>       the identifier type of the owned entity
 * @param <INPUT>    the input DTO type used for create/update operations
 * @param <OUTPUT>   the output representation (DTO, projection, view model, etc.)
 */
public abstract class OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> implements OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> {

    protected final Class<ENTITY> entityClass;
    protected final CrudEvents<ENTITY, ID, INPUT> events;

    /**
     * Creates a new owned CRUD provider for the given entity type.
     *
     * @param entityClass the entity class handled by this provider
     */
    protected OwnedEntityCrudProvider(Class<ENTITY> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "Entity class must not be null");
        this.events = registerEvents();
    }

    // ------------------------------------------------------------
    // CRUD OPERATIONS (public API)
    // ------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>
     * Normalizes the search input, resolves the page within the owner's scope,
     * triggers lifecycle events, and maps entity results to output DTOs.
     * </p>
     */
    @Override
    public Page<OUTPUT> page(@NotNull OWNER_ID ownerId, String search, String query, Pagination pagination, Sort sort, CrudOptions options) throws NotFoundException {
        preProcess(CrudOperation.PAGE);

        var normalized = StringUtils.normalize(search);
        var page = resolvePage(ownerId, normalized, pagination, sort, query, options);

        events.onPage(page);
        page.getContent().forEach(events::eachEntity);

        postProcess(CrudOperation.PAGE);
        return page.map(entity -> mapOutput(entity, options));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Retrieves the entity within the owner's scope, triggers lifecycle events,
     * and maps it to the output representation.
     * </p>
     */
    @Override
    public OUTPUT find(@NotNull OWNER_ID ownerId, @NotNull ID id, CrudOptions options) throws NotFoundException {
        preProcess(CrudOperation.FIND);

        var entity = internalFind(ownerId, id, options);

        events.onFind(entity);
        events.eachEntity(entity);

        postProcess(CrudOperation.FIND);
        return mapOutput(entity, options);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Normalizes the search text, resolves the count within the owner's scope,
     * triggers events, and returns the total number of matching entities.
     * </p>
     */
    @Override
    public long count(@NotNull OWNER_ID ownerId, String search, String query, CrudOptions options) throws NotFoundException {
        preProcess(CrudOperation.COUNT);

        var normalized = StringUtils.normalize(search);
        var count = resolveCount(ownerId, normalized, query, options);

        events.onCount(count);

        postProcess(CrudOperation.COUNT);
        return count;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Delegates to {@link #internalExists(OWNER_ID, ID, CrudOptions)}, triggers lifecycle
     * events, and returns whether the entity exists within the owner's scope.
     * </p>
     */
    @Override
    public boolean exists(@NotNull OWNER_ID ownerId, @NotNull ID id, CrudOptions options) {
        preProcess(CrudOperation.EXISTS);

        var exists = internalExists(ownerId, id, options);

        events.onExists(exists, id);

        postProcess(CrudOperation.EXISTS);
        return exists;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Performs the full creation workflow within the owner's scope:
     * </p>
     * <ol>
     *   <li>Creates a new empty entity instance via {@link #newEntity(CrudOptions)}</li>
     *   <li>Maps input DTO fields into the entity</li>
     *   <li>Applies "before create" lifecycle events</li>
     *   <li>Delegates persistence to {@link #internalCreate(OWNER_ID, ENTITY, CrudOptions)}</li>
     *   <li>Applies "after create" lifecycle events</li>
     *   <li>Maps entity to output DTO</li>
     * </ol>
     */
    @Override
    public OUTPUT create(@NotNull OWNER_ID ownerId, @NotNull @Valid INPUT input, CrudOptions options) throws NotFoundException {
        preProcess(CrudOperation.CREATE);

        var result = withTransaction(() -> {
            var entity = newEntity(options);

            mapInput(input, entity, true, options);
            events.onBeforeCreate(input, entity);

            var created = internalCreate(ownerId, entity, options);

            events.onAfterCreate(input, created);
            events.eachEntity(created);

            return mapOutput(created, options);
        });

        postProcess(CrudOperation.CREATE);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Performs the full update workflow within the owner's scope:
     * </p>
     * <ol>
     *   <li>Loads the existing entity scoped to the owner</li>
     *   <li>Maps updated DTO fields</li>
     *   <li>Triggers lifecycle events</li>
     *   <li>Delegates to {@link #internalUpdate(OWNER_ID, ENTITY, CrudOptions)}</li>
     *   <li>Maps updated entity to output DTO</li>
     * </ol>
     */
    @Override
    public OUTPUT update(@NotNull OWNER_ID ownerId, @NotNull ID id, @NotNull @Valid INPUT input, CrudOptions options) throws NotFoundException {
        preProcess(CrudOperation.UPDATE);

        var result = withTransaction(() -> {
            var entity = internalFind(ownerId, id, options);

            mapInput(input, entity, false, options);
            events.onBeforeUpdate(input, entity);

            var updated = internalUpdate(ownerId, entity, options);

            events.onAfterUpdate(input, updated);
            events.eachEntity(updated);

            return mapOutput(updated, options);
        });

        postProcess(CrudOperation.UPDATE);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Performs deletion within the owner's scope:
     * </p>
     * <ol>
     *   <li>Finds the entity scoped to the owner</li>
     *   <li>Triggers "before delete" event</li>
     *   <li>Delegates deletion to {@link #internalDelete(OWNER_ID, ENTITY, CrudOptions)}</li>
     *   <li>Triggers "after delete" event</li>
     * </ol>
     */
    @Override
    public void delete(@NotNull OWNER_ID ownerId, @NotNull ID id, CrudOptions options) throws NotFoundException {
        preProcess(CrudOperation.DELETE);

        withTransaction(() -> {
            var entity = internalFind(ownerId, id, options);

            events.onBeforeDelete(entity);

            internalDelete(ownerId, entity, options);

            events.onAfterDelete(entity);
            return null;
        });

        postProcess(CrudOperation.DELETE);
    }

    // ------------------------------------------------------------
    // ABSTRACT MAPPING CONTRACTS
    // ------------------------------------------------------------

    /**
     * Maps the contents of the input DTO into the given entity instance.
     *
     * @param input   the input DTO
     * @param entity  the entity to populate
     * @param isNew   whether this is a creation (true) or update (false)
     * @param options execution modifiers
     */
    protected abstract void mapInput(INPUT input, ENTITY entity, boolean isNew, CrudOptions options);

    /**
     * Converts an entity into its output DTO representation.
     *
     * @param entity  the entity to convert
     * @param options execution modifiers
     * @return the mapped output DTO
     */
    protected abstract OUTPUT mapOutput(ENTITY entity, CrudOptions options);

    // ------------------------------------------------------------
    // ABSTRACT PERSISTENCE CONTRACTS
    // ------------------------------------------------------------

    /**
     * Retrieves an entity by its identifier within the owner's scope.
     *
     * <p>
     * Implementations must filter by both {@code ownerId} and {@code id} to ensure
     * the entity actually belongs to the given owner.
     * </p>
     *
     * @param ownerId the identifier of the owning resource
     * @param id      the identifier of the entity
     * @param options execution modifiers
     * @return the found entity
     * @throws NotFoundException if no entity matches both identifiers
     */
    protected abstract ENTITY internalFind(OWNER_ID ownerId, ID id, CrudOptions options) throws NotFoundException;

    /**
     * Retrieves a page of entities belonging to the given owner.
     *
     * @param ownerId    the identifier of the owning resource
     * @param search     normalized search string
     * @param query      processed query expression
     * @param pagination pagination settings
     * @param sort       sorting configuration
     * @param options    execution modifiers
     * @return a page of matching entities
     */
    protected abstract Page<ENTITY> internalPage(OWNER_ID ownerId, String search, String query, Pagination pagination, Sort sort, CrudOptions options);

    /**
     * Counts entities belonging to the given owner matching optional filters.
     *
     * @param ownerId the identifier of the owning resource
     * @param search  normalized search string
     * @param query   processed query expression
     * @param options execution modifiers
     * @return total count of matching entities
     */
    protected abstract long internalCount(OWNER_ID ownerId, String search, String query, CrudOptions options);

    /**
     * Checks whether an entity with the given identifier exists within the owner's scope.
     *
     * @param ownerId the identifier of the owning resource
     * @param id      the identifier of the entity
     * @param options execution modifiers
     * @return {@code true} if the entity exists and belongs to the owner
     */
    protected abstract boolean internalExists(OWNER_ID ownerId, ID id, CrudOptions options);

    /**
     * Persists a new entity under the given owner.
     *
     * @param ownerId the identifier of the owning resource
     * @param entity  the entity to persist
     * @param options execution modifiers
     * @return the persisted entity
     */
    protected abstract ENTITY internalCreate(OWNER_ID ownerId, ENTITY entity, CrudOptions options);

    /**
     * Persists changes to an existing entity within the owner's scope.
     *
     * @param ownerId the identifier of the owning resource
     * @param entity  the entity with updated state
     * @param options execution modifiers
     * @return the updated entity
     */
    protected abstract ENTITY internalUpdate(OWNER_ID ownerId, ENTITY entity, CrudOptions options);

    /**
     * Removes an entity within the owner's scope.
     *
     * @param ownerId the identifier of the owning resource
     * @param entity  the entity to remove
     * @param options execution modifiers
     */
    protected abstract void internalDelete(OWNER_ID ownerId, ENTITY entity, CrudOptions options);

    // ------------------------------------------------------------
    // EXTENSION HOOKS
    // ------------------------------------------------------------

    /**
     * Hook executed before a CRUD operation begins.
     *
     * <p>
     * Subclasses may override to add validation, logging, permission checks,
     * or other cross-cutting concerns.
     * </p>
     *
     * @param operation the operation being executed
     */
    protected void preProcess(CrudOperation operation) {
        // Subclasses may override
    }

    /**
     * Hook executed after a CRUD operation completes.
     *
     * @param operation the completed CRUD operation
     */
    protected void postProcess(CrudOperation operation) {
        // Subclasses may override
    }

    // ------------------------------------------------------------
    // UTILITIES
    // ------------------------------------------------------------

    /**
     * Creates a new instance of the managed entity type using its default constructor.
     *
     * <p>
     * Subclasses may override when entities require factory methods instead of reflection.
     * </p>
     *
     * @param options execution modifiers
     * @return a new entity instance
     */
    protected ENTITY newEntity(CrudOptions options) {
        try {
            return entityClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate entity: " + entityClass.getName(), e);
        }
    }

    /**
     * Wraps a function in a transaction context.
     *
     * <p>
     * Default implementation executes the function directly, but subclasses may
     * override to begin/commit/rollback an actual transactional boundary (e.g., JPA,
     * Spring, Hibernate, Quarkus, JDBC).
     * </p>
     *
     * @param function the function to execute
     * @param <T>      the return type
     * @return the function result
     */
    protected <T> T withTransaction(Supplier<T> function) {
        return function.get();
    }

    /**
     * Registers the CRUD lifecycle events handler.
     *
     * <p>
     * Reuses the same default events contract as {@link EntityCrudProvider}.
     * Subclasses may override to provide a custom implementation.
     * </p>
     *
     * @return the CRUD lifecycle events handler
     */
    protected CrudEvents<ENTITY, ID, INPUT> registerEvents() {
        return CrudEvents.getDefault();
    }

    /**
     * Processes the raw query string before use.
     *
     * @param query the raw query string
     * @return the processed query string
     */
    protected String applyQueryPolicies(OWNER_ID ownerId, String query) {
        return query;
    }

    // ------------------------------------------------------------
    // PRIVATE HELPERS
    // ------------------------------------------------------------

    private Page<ENTITY> resolvePage(OWNER_ID ownerId, String search, Pagination pagination, Sort sort, String query, CrudOptions options) {
        if (pagination == null) pagination = Pagination.unpaginated();
        if (sort == null) sort = Sort.unsorted();
        var newQuery = applyQueryPolicies(ownerId, query);
        return internalPage(ownerId, search, newQuery, pagination, sort, options);
    }

    private long resolveCount(OWNER_ID ownerId, String search, String query, CrudOptions options) {
        var newQuery = applyQueryPolicies(ownerId, query);
        return internalCount(ownerId, search, newQuery, options);
    }
}