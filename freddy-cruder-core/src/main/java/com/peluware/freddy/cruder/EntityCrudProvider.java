package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.freddy.cruder.utils.StringUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.function.Supplier;

/**
 * A high-level, framework-agnostic CRUD provider that encapsulates the full lifecycle of
 * entity persistence operations (create, read, update, delete) while delegating the
 * low-level storage logic to subclasses.
 *
 * <p>
 * This abstraction provides a unified workflow with:
 * </p>
 *
 * <ul>
 *   <li>Standardized CRUD operations defined by {@link CrudProvider}</li>
 *   <li>Entity/DTO mapping hooks via {@link #mapInput(INPUT, ENTITY, boolean)} and {@link #mapOutput(ENTITY)}</li>
 *   <li>Automatic invocation of CRUD lifecycle event callbacks via {@link CrudEvents}</li>
 *   <li>Pre- and post-operation hooks for cross-cutting concerns</li>
 *   <li>Optional transaction wrapping through {@link #withTransaction(Supplier)}</li>
 * </ul>
 *
 * <p>
 * Concrete implementations must supply the underlying persistence logic (e.g., JPA, JDBC,
 * MongoDB, in-memory storage) by implementing the {@code internal*} methods such as
 * {@link #internalFind(Object)}, {@link #internalCreate(Object)}, and others.
 * </p>
 *
 * <p>
 * This class is not tied to any specific storage technology and can be used in
 * hexagonal/clean architectures, domain-driven designs, or layered architectures.
 * </p>
 *
 * @param <ENTITY> the domain entity type managed by this provider
 * @param <ID>     the identifier type of the entity
 * @param <INPUT>  the input DTO type used for create/update operations
 * @param <OUTPUT> the output representation (DTO, projection, view model, etc.)
 */
public abstract class EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> implements CrudProvider<ID, INPUT, OUTPUT> {

    protected final Class<ENTITY> entityClass;
    private final CrudEvents<ENTITY, ID, INPUT> events;

    /**
     * Creates a new CRUD provider for the given entity type using custom CRUD lifecycle events.
     *
     * @param entityClass the entity class handled by this provider
     * @param events      the event dispatcher used to report CRUD lifecycle events
     */
    protected EntityCrudProvider(Class<ENTITY> entityClass, CrudEvents<ENTITY, ID, INPUT> events) {
        this.entityClass = entityClass;
        this.events = events;
    }

    /**
     * Creates a new CRUD provider using default {@link CrudEvents}.
     *
     * @param entityClass the entity class handled by this provider
     */
    protected EntityCrudProvider(Class<ENTITY> entityClass) {
        this(entityClass, CrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // CRUD OPERATIONS (public API)
    // ------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation normalizes search input, resolves the correct page retrieval strategy
     * (simple paging or search mode), triggers lifecycle events, and maps entity results to
     * output DTOs.
     * </p>
     */
    @Override
    public final Page<OUTPUT> page(String search, Pagination pagination, Sort sort, String query) {
        preProcess(CrudOperation.PAGE);

        var normalized = StringUtils.normalize(search);
        var page = resolvePage(normalized, pagination, sort, query);

        events.onPage(page);
        page.getContent().forEach(events::eachEntity);

        postProcess(CrudOperation.PAGE);
        return page.map(this::mapOutput);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation retrieves the entity, triggers lifecycle events, and maps it
     * to the output representation.
     * </p>
     */
    @Override
    public final OUTPUT find(@NotNull ID id) throws NotFoundEntityException {
        preProcess(CrudOperation.FIND);

        var entity = internalFind(id);

        events.onFind(entity);
        events.eachEntity(entity);

        postProcess(CrudOperation.FIND);
        return mapOutput(entity);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation normalizes the search text, resolves the appropriate count strategy,
     * triggers events, and returns the total number of matching entities.
     * </p>
     */
    @Override
    public final long count(String search, String query) {
        preProcess(CrudOperation.COUNT);

        var normalized = StringUtils.normalize(search);
        var count = resolveCount(normalized, query);

        events.onCount(count);

        postProcess(CrudOperation.COUNT);
        return count;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation delegates to {@link #internalExists(Object)}, triggers lifecycle
     * events, and returns whether the entity exists.
     * </p>
     */
    @Override
    public final boolean exists(@NotNull ID id) {
        preProcess(CrudOperation.EXISTS);

        var exists = internalExists(id);

        events.onExists(exists, id);

        postProcess(CrudOperation.EXISTS);
        return exists;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation performs the full creation workflow:
     * </p>
     * <ol>
     *   <li>Creates a new empty entity instance via {@link #newEntity()}</li>
     *   <li>Maps input DTO fields into the entity</li>
     *   <li>Applies "before create" lifecycle events</li>
     *   <li>Delegates persistence to {@link #internalCreate(Object)}</li>
     *   <li>Applies "after create" lifecycle events</li>
     *   <li>Maps entity to output DTO</li>
     * </ol>
     */
    @Override
    public final OUTPUT create(@NotNull @Valid INPUT input) {
        preProcess(CrudOperation.CREATE);

        var result = withTransaction(() -> {
            var entity = newEntity();

            mapInput(input, entity, true);
            events.onBeforeCreate(input, entity);

            var created = internalCreate(entity);

            events.onAfterCreate(input, created);
            events.eachEntity(created);

            return mapOutput(created);
        });

        postProcess(CrudOperation.CREATE);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation performs the full update workflow:
     * </p>
     * <ol>
     *   <li>Loads the existing entity</li>
     *   <li>Maps updated DTO fields</li>
     *   <li>Triggers lifecycle events</li>
     *   <li>Delegates to {@link #internalUpdate(Object)}</li>
     *   <li>Maps updated entity to output DTO</li>
     * </ol>
     */
    @Override
    public final OUTPUT update(@NotNull ID id, @NotNull @Valid INPUT input) throws NotFoundEntityException {
        preProcess(CrudOperation.UPDATE);

        var result = withTransaction(() -> {
            var entity = internalFind(id);

            mapInput(input, entity, false);
            events.onBeforeUpdate(input, entity);

            var updated = internalUpdate(entity);

            events.onAfterUpdate(input, updated);
            events.eachEntity(updated);

            return mapOutput(updated);
        });

        postProcess(CrudOperation.UPDATE);
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This implementation:
     * </p>
     * <ol>
     *   <li>Finds the entity</li>
     *   <li>Triggers "before delete" event</li>
     *   <li>Delegates deletion to {@link #internalDelete(Object)}</li>
     *   <li>Triggers "after delete" event</li>
     * </ol>
     */
    @Override
    public final void delete(@NotNull ID id) throws NotFoundEntityException {
        preProcess(CrudOperation.DELETE);

        withTransaction(() -> {
            var entity = internalFind(id);

            events.onBeforeDelete(entity);
            internalDelete(entity);
            events.onAfterDelete(entity);

            events.eachEntity(entity);
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
     * @param input  the input DTO
     * @param entity the entity to populate
     * @param isNew  whether this is a creation (true) or update (false)
     */
    protected abstract void mapInput(INPUT input, ENTITY entity, boolean isNew);

    /**
     * Converts an entity into its output DTO representation.
     *
     * @param entity the entity to convert
     * @return the mapped output DTO
     */
    protected abstract OUTPUT mapOutput(ENTITY entity);

    // ------------------------------------------------------------
    // ABSTRACT PERSISTENCE CONTRACTS
    // ------------------------------------------------------------

    protected abstract ENTITY internalFind(ID id) throws NotFoundEntityException;

    protected abstract Page<ENTITY> internalPage(Pagination pagination, Sort sort);

    protected abstract Page<ENTITY> internalSearch(String search, Pagination pagination, Sort sort);

    protected abstract Page<ENTITY> internalSearch(String search, Pagination pagination, Sort sort, String query);

    protected abstract long internalCount(String search, String query);

    protected abstract long internalCount(String search);

    protected abstract long internalCount();

    protected abstract boolean internalExists(ID id);

    protected abstract ENTITY internalCreate(ENTITY entity);

    protected abstract ENTITY internalUpdate(ENTITY entity);

    protected abstract void internalDelete(ENTITY entity);

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

    private Page<ENTITY> resolvePage(String search, Pagination pagination, Sort sort, String query) {
        if (pagination == null) pagination = Pagination.unpaginated();
        if (sort == null) sort = Sort.unsorted();
        if (query == null) {
            return StringUtils.isEmpty(search)
                    ? internalPage(pagination, sort)
                    : internalSearch(search, pagination, sort);
        }
        return internalSearch(search, pagination, sort, query);
    }

    private long resolveCount(String search, String query) {
        if (query == null) {
            return StringUtils.isEmpty(search)
                    ? internalCount()
                    : internalCount(search);
        }
        return internalCount(search, query);
    }
}
