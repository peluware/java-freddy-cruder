package com.peluware.freddy.cruder.memory;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.NotFoundEntityException;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An {@link EntityCrudProvider} that keeps its entities in memory.
 * Create, update and delete run in a simulated transaction that is rolled back if it fails.
 *
 * @param <ENTITY> the domain entity type
 * @param <ID>     the identifier type of the entity
 * @param <INPUT>  the input DTO type used for create/update operations
 * @param <OUTPUT> the output representation
 */
public abstract class MemoryCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    private final MemoryStore<ENTITY, ID> store;

    /**
     * @param ids         how identifiers are read and generated
     * @param entityClass the entity class handled by this provider
     * @param events      the CRUD lifecycle events handler
     */
    protected MemoryCrudProvider(MemoryIds<ENTITY, ID> ids, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.store = new MemoryStore<>(ids);
    }

    /**
     * @param ids         how identifiers are read and generated
     * @param entityClass the entity class handled by this provider
     */
    protected MemoryCrudProvider(MemoryIds<ENTITY, ID> ids, Class<ENTITY> entityClass) {
        this(ids, entityClass, EntityCrudEvents.getDefault());
    }

    /**
     * Resolves the entity class from the generic type hierarchy.
     *
     * @param ids    how identifiers are read and generated
     * @param events the CRUD lifecycle events handler
     */
    protected MemoryCrudProvider(MemoryIds<ENTITY, ID> ids, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.store = new MemoryStore<>(ids);
    }

    /**
     * Resolves the entity class from the generic type hierarchy.
     *
     * @param ids how identifiers are read and generated
     */
    protected MemoryCrudProvider(MemoryIds<ENTITY, ID> ids) {
        this(ids, EntityCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // TEST SUPPORT
    // ------------------------------------------------------------

    /**
     * Stores entities as they are, without mapping or events.
     *
     * @param entities the entities to store
     */
    @SafeVarargs
    public final void seed(ENTITY... entities) {
        for (var entity : entities) {
            store.insert(entity);
        }
    }

    /**
     * @return a snapshot of every stored entity, in insertion order
     */
    public List<ENTITY> entities() {
        return store.all();
    }

    /**
     * Removes every stored entity.
     */
    public void clear() {
        store.clear();
    }

    /**
     * Runs work in a simulated transaction, restoring the stored entities if it throws. Nested calls
     * join the outer transaction.
     *
     * @param work the work to run
     * @param <T>  the type of its result
     * @return the result of the work
     */
    public <T extends @Nullable Object> T transactional(Supplier<T> work) {
        return store.transactional(work);
    }

    /**
     * Runs work that returns nothing in a simulated transaction.
     *
     * @param work the work to run
     */
    public void inTransaction(Runnable work) {
        store.<@Nullable Object>transactional(() -> {
            work.run();
            return null;
        });
    }

    @Override
    protected <T> T withTransaction(Supplier<T> function) {
        return store.transactional(function);
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        var entity = store.get(id);
        if (entity == null) {
            throw new NotFoundEntityException(entityClass, id);
        }
        return entity;
    }

    @Override
    protected Page<ENTITY> internalPage(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        return store.page(searchPredicate(search, query), pagination, sort);
    }

    @Override
    protected List<ENTITY> internalList(@Nullable String search, @Nullable String query, Sort sort) {
        return store.matching(searchPredicate(search, query), sort);
    }

    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return store.all().stream().filter(searchPredicate(search, query)).count();
    }

    @Override
    protected boolean internalExists(ID id) {
        return store.contains(id);
    }

    @Override
    protected ENTITY internalCreate(ENTITY entity) {
        return store.insert(entity);
    }

    @Override
    protected ENTITY internalUpdate(ENTITY entity) {
        return store.replace(entity);
    }

    @Override
    protected void internalDelete(ENTITY entity) {
        store.remove(entity);
    }

    // ------------------------------------------------------------
    // HOOKS
    // ------------------------------------------------------------

    /**
     * Builds the filter for a search and query.
     *
     * @param search the normalized search text, or {@code null}
     * @param query  the query, or {@code null}
     * @return the filter; matches every entity by default
     */
    protected Predicate<ENTITY> searchPredicate(@Nullable String search, @Nullable String query) {
        return _ -> true;
    }
}
