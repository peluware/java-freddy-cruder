package com.peluware.freddy.cruder.memory;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.NotFoundException;
import com.peluware.freddy.cruder.OwnedEntityCrudProvider;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An {@link OwnedEntityCrudProvider} that keeps its entities in memory.
 * Create, update and delete run in a simulated transaction that is rolled back if it fails.
 *
 * @param <ENTITY>   the domain entity type
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the identifier type of the entity
 * @param <INPUT>    the input DTO type used for create/update operations
 * @param <OUTPUT>   the output representation
 */
public abstract class OwnedMemoryCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> extends OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> {

    private final MemoryStore<ENTITY, ID> store;

    /**
     * @param ids         how identifiers are read and generated
     * @param entityClass the entity class handled by this provider
     * @param events      the CRUD lifecycle events handler
     */
    protected OwnedMemoryCrudProvider(MemoryIds<ENTITY, ID> ids, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.store = new MemoryStore<>(ids);
    }

    /**
     * @param ids         how identifiers are read and generated
     * @param entityClass the entity class handled by this provider
     */
    protected OwnedMemoryCrudProvider(MemoryIds<ENTITY, ID> ids, Class<ENTITY> entityClass) {
        this(ids, entityClass, EntityCrudEvents.getDefault());
    }

    /**
     * Resolves the entity class from the generic type hierarchy.
     *
     * @param ids    how identifiers are read and generated
     * @param events the CRUD lifecycle events handler
     */
    protected OwnedMemoryCrudProvider(MemoryIds<ENTITY, ID> ids, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.store = new MemoryStore<>(ids);
    }

    /**
     * Resolves the entity class from the generic type hierarchy.
     *
     * @param ids how identifiers are read and generated
     */
    protected OwnedMemoryCrudProvider(MemoryIds<ENTITY, ID> ids) {
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
     * @return a snapshot of every stored entity of every owner, in insertion order
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
     * Runs work in a simulated transaction, restoring the stored entities if it throws.
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
    protected ENTITY internalFind(OWNER_ID ownerId, ID id) throws NotFoundException {
        var entity = store.get(id);
        if (entity == null || !belongsTo(ownerId, entity)) {
            throw new NotFoundEntityException(entityClass, id);
        }
        return entity;
    }

    @Override
    protected Page<ENTITY> internalPage(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        return store.page(scoped(ownerId, search, query), pagination, sort);
    }

    @Override
    protected List<ENTITY> internalList(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Sort sort) {
        return store.matching(scoped(ownerId, search, query), sort);
    }

    @Override
    protected long internalCount(OWNER_ID ownerId, @Nullable String search, @Nullable String query) {
        return store.all().stream().filter(scoped(ownerId, search, query)).count();
    }

    @Override
    protected boolean internalExists(OWNER_ID ownerId, ID id) {
        var entity = store.get(id);
        return entity != null && belongsTo(ownerId, entity);
    }

    @Override
    protected ENTITY internalCreate(OWNER_ID ownerId, ENTITY entity) {
        return store.insert(entity);
    }

    @Override
    protected ENTITY internalUpdate(OWNER_ID ownerId, ENTITY entity) {
        return store.replace(entity);
    }

    @Override
    protected void internalDelete(OWNER_ID ownerId, ENTITY entity) {
        store.remove(entity);
    }

    // ------------------------------------------------------------
    // HOOKS
    // ------------------------------------------------------------

    /**
     * Whether an entity belongs to an owner.
     *
     * @param ownerId the identifier of the owning resource
     * @param entity  the entity
     * @return whether it belongs to that owner
     */
    protected abstract boolean belongsTo(OWNER_ID ownerId, ENTITY entity);

    /**
     * Builds the filter for a search and query within an owner.
     *
     * @param ownerId the identifier of the owning resource
     * @param search  the normalized search text, or {@code null}
     * @param query   the query, or {@code null}
     * @return the filter; matches every entity by default
     */
    protected Predicate<ENTITY> searchPredicate(OWNER_ID ownerId, @Nullable String search, @Nullable String query) {
        return _ -> true;
    }

    private Predicate<ENTITY> scoped(OWNER_ID ownerId, @Nullable String search, @Nullable String query) {
        return searchPredicate(ownerId, search, query).and(entity -> belongsTo(ownerId, entity));
    }
}
