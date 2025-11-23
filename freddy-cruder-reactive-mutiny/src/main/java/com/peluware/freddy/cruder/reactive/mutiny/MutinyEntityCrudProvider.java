package com.peluware.freddy.cruder.reactive.mutiny;


import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.CrudOperation;
import com.peluware.freddy.cruder.utils.StringUtils;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.function.Supplier;


/**
 * A high-level reactive, framework-agnostic CRUD provider that encapsulates the full lifecycle of
 * entity persistence operations (create, read, update, delete) in a non-blocking,
 * {@link io.smallrye.mutiny.Uni}-based API.
 *
 * <p>
 * This abstraction provides a unified reactive workflow with:
 * </p>
 *
 * <ul>
 *   <li>Standardized CRUD operations defined by {@link MutinyCrudProvider}</li>
 *   <li>Entity/DTO mapping hooks via {@link #mapEntity(INPUT, ENTITY, boolean)} and {@link #mapOutput(ENTITY)}</li>
 *   <li>Automatic invocation of reactive CRUD lifecycle events via {@link MutinyCrudEvents}</li>
 *   <li>Pre- and post-operation hooks for cross-cutting concerns in a reactive context</li>
 *   <li>Optional reactive transaction wrapping through {@link #withTransaction(Supplier)}</li>
 * </ul>
 *
 * <p>
 * Concrete implementations must supply the underlying reactive persistence logic (e.g., reactive
 * Hibernate, R2DBC, reactive MongoDB) by implementing the {@code internal*} methods such as
 * {@link #internalFind(Object)}, {@link #internalCreate(Object)}, and others.
 * </p>
 *
 * <p>
 * This class allows building fully reactive CRUD services that integrate cleanly with
 * non-blocking frameworks and reactive pipelines while maintaining a consistent API and
 * lifecycle hooks.
 * </p>
 *
 * @param <ENTITY> the domain entity type managed by this provider
 * @param <ID>     the identifier type of the entity
 * @param <INPUT>  the input DTO type used for create/update operations
 * @param <OUTPUT> the output representation (DTO, projection, view model, etc.)
 */
public abstract class MutinyEntityCrudProvider<ENTITY, ID, INPUT, OUTPUT>
        implements MutinyCrudProvider<ID, INPUT, OUTPUT> {

    protected final Class<ENTITY> entityClass;
    private final MutinyCrudEvents<ENTITY, ID, INPUT> events;

    /**
     * Creates a new reactive CRUD provider for the given entity type using custom lifecycle events.
     *
     * @param entityClass the entity class handled by this provider
     * @param events      the reactive event dispatcher used to report CRUD lifecycle events
     */
    protected MutinyEntityCrudProvider(Class<ENTITY> entityClass, MutinyCrudEvents<ENTITY, ID, INPUT> events) {
        this.entityClass = entityClass;
        this.events = events;
    }

    /**
     * Creates a new reactive CRUD provider using default {@link MutinyCrudEvents}.
     *
     * @param entityClass the entity class handled by this provider
     */
    protected MutinyEntityCrudProvider(Class<ENTITY> entityClass) {
        this(entityClass, MutinyCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // REACTIVE CRUD OPERATIONS (public API)
    // ------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>
     * This reactive implementation normalizes search input, resolves the correct page retrieval strategy,
     * triggers lifecycle events in a reactive pipeline, and maps entity results to output DTOs
     * using {@link PageUtils#map(Page, java.util.function.Function)}.
     * </p>
     */
    @Override
    public final Uni<Page<OUTPUT>> page(String search, Pagination pagination, Sort sort, String query) {
        return preProcess(CrudOperation.PAGE)
                .onItem().transformToUni(v -> resolvePage(StringUtils.normalize(search), pagination, sort, query))
                .onItem().call(events::onPage)
                .onItem().invoke(page -> page.getContent().forEach(events::eachEntity))
                .onItem().call(page -> postProcess(CrudOperation.PAGE))
                .onItem().transformToUni(page -> PageUtils.map(page, this::mapOutput));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * This reactive implementation retrieves the entity, triggers lifecycle events,
     * and maps it to the output representation, all in a non-blocking manner.
     * </p>
     */
    @Override
    public final Uni<OUTPUT> find(@NotNull ID id) {
        return preProcess(CrudOperation.FIND)
                .onItem().transformToUni(v -> internalFind(id))
                .onItem().call(events::onFind)
                .onItem().invoke(events::eachEntity)
                .onItem().call(v -> postProcess(CrudOperation.FIND))
                .onItem().transformToUni(this::mapOutput);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Counts entities reactively, resolving search/query strategies and triggering
     * reactive lifecycle events.
     * </p>
     */
    @Override
    public final Uni<Long> count(String search, String query) {
        return preProcess(CrudOperation.COUNT)
                .onItem().transformToUni(v -> resolveCount(StringUtils.normalize(search), query))
                .onItem().call(events::onCount)
                .onItem().call(v -> postProcess(CrudOperation.COUNT));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Checks entity existence in a reactive manner and triggers lifecycle events.
     * </p>
     */
    @Override
    public final Uni<Boolean> exists(@NotNull ID id) {
        return preProcess(CrudOperation.EXISTS)
                .onItem().transformToUni(v -> internalExists(id))
                .onItem().call(exists -> events.onExists(exists, id))
                .onItem().call(exists -> postProcess(CrudOperation.EXISTS));
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Executes the full creation workflow reactively:
     * </p>
     * <ol>
     *   <li>Creates a new empty entity via {@link #newEntity()}</li>
     *   <li>Maps the input DTO into the entity using {@link #mapEntity(INPUT, ENTITY, boolean)}</li>
     *   <li>Triggers "before create" reactive events</li>
     *   <li>Persists the entity with {@link #internalCreate(ENTITY)}</li>
     *   <li>Triggers "after create" reactive events</li>
     *   <li>Maps the persisted entity to output DTO</li>
     * </ol>
     */
    @Override
    public final Uni<OUTPUT> create(@NotNull @Valid INPUT input) {
        return preProcess(CrudOperation.CREATE)
                .onItem().transformToUni(v -> withTransaction(() -> {
                    var entity = newEntity();
                    return mapEntity(input, entity, true)
                            .onItem().call(vv -> events.onBeforeCreate(input, entity))
                            .onItem().transformToUni(vv -> internalCreate(entity))
                            .onItem().call(created -> events.onAfterCreate(input, created))
                            .onItem().invoke(events::eachEntity);
                }))
                .onItem().call(created -> postProcess(CrudOperation.CREATE))
                .onItem().transformToUni(this::mapOutput);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Executes the full update workflow reactively:
     * </p>
     * <ol>
     *   <li>Finds the existing entity</li>
     *   <li>Maps updated DTO fields</li>
     *   <li>Triggers "before update" reactive events</li>
     *   <li>Persists updates with {@link #internalUpdate(ENTITY)}</li>
     *   <li>Triggers "after update" reactive events</li>
     *   <li>Maps the updated entity to output DTO</li>
     * </ol>
     */
    @Override
    public final Uni<OUTPUT> update(@NotNull ID id, @NotNull @Valid INPUT input) {
        return preProcess(CrudOperation.UPDATE)
                .onItem().transformToUni(v -> withTransaction(() ->
                        internalFind(id)
                                .onItem().call(entity -> mapEntity(input, entity, false))
                                .onItem().call(entity -> events.onBeforeUpdate(input, entity))
                                .onItem().transformToUni(this::internalUpdate)
                                .onItem().call(updated -> events.onAfterUpdate(input, updated))
                                .onItem().invoke(events::eachEntity)
                ))
                .onItem().call(unused -> postProcess(CrudOperation.UPDATE))
                .onItem().transformToUni(this::mapOutput);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Executes the full delete workflow reactively:
     * </p>
     * <ol>
     *   <li>Finds the entity</li>
     *   <li>Triggers "before delete" reactive events</li>
     *   <li>Deletes the entity with {@link #internalDelete(ENTITY)}</li>
     *   <li>Triggers "after delete" reactive events</li>
     * </ol>
     */
    @Override
    public final Uni<Void> delete(@NotNull ID id) {
        return preProcess(CrudOperation.DELETE)
                .onItem().transformToUni(v -> withTransaction(() ->
                        internalFind(id)
                                .onItem().call(events::onBeforeDelete)
                                .onItem().call(this::internalDelete)
                                .onItem().call(events::onAfterDelete)
                                .onItem().invoke(events::eachEntity)
                                .replaceWithVoid()
                ))
                .onItem().call(unused -> postProcess(CrudOperation.DELETE));
    }

    // ------------------------------------------------------------
    // ABSTRACT MAPPING CONTRACTS
    // ------------------------------------------------------------

    /**
     * Maps the input DTO into the given entity reactively.
     *
     * @param input  the input DTO
     * @param entity the entity to populate
     * @param isNew  whether this is a creation (true) or update (false)
     * @return a {@link Uni} completing when the mapping is done
     */
    protected abstract Uni<Void> mapEntity(INPUT input, ENTITY entity, boolean isNew);

    /**
     * Converts an entity into its output DTO reactively.
     *
     * @param entity the entity to convert
     * @return a {@link Uni} emitting the mapped output DTO
     */
    protected abstract Uni<OUTPUT> mapOutput(ENTITY entity);

    // ------------------------------------------------------------
    // ABSTRACT PERSISTENCE CONTRACTS
    // ------------------------------------------------------------

    protected abstract Uni<Page<ENTITY>> internalPage(Pagination pagination, Sort sort);

    protected abstract Uni<Page<ENTITY>> internalSearch(String search, Pagination pagination, Sort sort);

    protected abstract Uni<Page<ENTITY>> internalSearch(String search, Pagination pagination, Sort sort, String query);

    protected abstract Uni<Long> internalCount(String search, String query);

    protected abstract Uni<Long> internalCount(String search);

    protected abstract Uni<Long> internalCount();

    protected abstract Uni<Boolean> internalExists(ID id);

    protected abstract Uni<ENTITY> internalFind(ID id);

    protected abstract Uni<ENTITY> internalCreate(ENTITY entity);

    protected abstract Uni<ENTITY> internalUpdate(ENTITY entity);

    protected abstract Uni<Void> internalDelete(ENTITY entity);

    // ------------------------------------------------------------
    // EXTENSION HOOKS
    // ------------------------------------------------------------

    /**
     * Hook executed before a reactive CRUD operation begins.
     *
     * @param operation the CRUD operation being executed
     * @return a {@link Uni} completing when pre-processing is done
     */
    protected Uni<Void> preProcess(CrudOperation operation) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Hook executed after a reactive CRUD operation completes.
     *
     * @param operation the CRUD operation that completed
     * @return a {@link Uni} completing when post-processing is done
     */
    protected Uni<Void> postProcess(CrudOperation operation) {
        return Uni.createFrom().voidItem();
    }

    // ------------------------------------------------------------
    // UTILITIES
    // ------------------------------------------------------------

    /**
     * Creates a new instance of the managed entity type using its default constructor.
     *
     * <p>
     * Subclasses may override if entities require factory methods instead of reflection.
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
     * Wraps a reactive function in a transaction context.
     *
     * <p>
     * Default implementation executes the supplier directly, but subclasses may override
     * to provide real transactional boundaries.
     * </p>
     *
     * @param function the reactive function to execute
     * @param <T>      the type of result emitted
     * @return a {@link Uni} emitting the result of the function
     */
    protected <T> Uni<T> withTransaction(Supplier<Uni<T>> function) {
        return function.get();
    }

    private Uni<Page<ENTITY>> resolvePage(String search, Pagination pagination, Sort sort, String query) {
        if (pagination == null) pagination = Pagination.unpaginated();
        if (sort == null) sort = Sort.unsorted();
        if (query == null) {
            return StringUtils.isEmpty(search)
                    ? internalPage(pagination, sort)
                    : internalSearch(search, pagination, sort);
        }
        return internalSearch(search, pagination, sort, query);
    }

    private Uni<Long> resolveCount(String search, String query) {
        if (query == null) {
            return StringUtils.isEmpty(search)
                    ? internalCount()
                    : internalCount(search);
        }
        return internalCount(search, query);
    }
}
