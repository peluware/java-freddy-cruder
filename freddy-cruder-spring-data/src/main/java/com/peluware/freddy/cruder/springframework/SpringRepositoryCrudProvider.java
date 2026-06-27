package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.NotFoundEntityException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.repository.CrudRepository;

/**
 * Spring Data implementation of {@link EntityCrudProvider} that delegates persistence
 * to a {@link CrudRepository} and search/pagination to an {@link SearchRepository}.
 *
 * <p>
 * When your repository implements both contracts (e.g. {@code extends JpaRepository<E,ID>, JpaSearchRepository<E>}),
 * use the single-argument constructors to avoid passing the same object twice.
 * </p>
 *
 * @param <ENTITY> the JPA entity type
 * @param <ID>     the entity identifier type
 * @param <INPUT>  the input DTO type for create/update operations
 * @param <OUTPUT> the output DTO or projection type
 */
public abstract class SpringRepositoryCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final CrudRepository<ENTITY, ID> repository;
    protected final SearchRepository<ENTITY> searchRepository;

    // ------------------------------------------------------------
    // CONSTRUCTORS — explicit entityClass
    // ------------------------------------------------------------

    /**
     * Creates a provider with explicit entity class, separate repository and search adapter.
     *
     * @param repository       the Spring Data repository for persistence operations
     * @param searchRepository the search and count adapter
     * @param entityClass      the entity class managed by this provider
     * @param events           the CRUD lifecycle events handler
     */
    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, SearchRepository<ENTITY> searchRepository, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.repository = repository;
        this.searchRepository = searchRepository;
    }

    /**
     * Creates a provider with explicit entity class and default lifecycle events (no-op).
     *
     * @param repository       the Spring Data repository for persistence operations
     * @param searchRepository the search and count adapter
     * @param entityClass      the entity class managed by this provider
     */
    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, SearchRepository<ENTITY> searchRepository, Class<ENTITY> entityClass) {
        this(repository, searchRepository, entityClass, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider with explicit entity class from a repository that implements both
     * {@link CrudRepository} and {@link SearchRepository} (e.g. one that extends both
     * {@code JpaRepository} and {@code JpaSearchRepository}).
     *
     * @param repository  the repository implementing both persistence and search
     * @param entityClass the entity class managed by this provider
     * @param events      the CRUD lifecycle events handler
     */
    public <R extends CrudRepository<ENTITY, ID> & SearchRepository<ENTITY>> SpringRepositoryCrudProvider(R repository, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        this(repository, repository, entityClass, events);
    }

    /**
     * Creates a provider with explicit entity class from a repository that implements both
     * {@link CrudRepository} and {@link SearchRepository}, with default lifecycle events.
     *
     * @param repository  the repository implementing both persistence and search
     * @param entityClass the entity class managed by this provider
     */
    public <R extends CrudRepository<ENTITY, ID> & SearchRepository<ENTITY>> SpringRepositoryCrudProvider(R repository, Class<ENTITY> entityClass) {
        this(repository, repository, entityClass, EntityCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // CONSTRUCTORS — reflection-based entityClass
    // ------------------------------------------------------------

    /**
     * Creates a provider by resolving the entity class automatically from the generic type
     * hierarchy via reflection, using separate repository and search adapter.
     *
     * @param repository       the Spring Data repository for persistence operations
     * @param searchRepository the search and count adapter
     * @param events           the CRUD lifecycle events handler
     */
    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, SearchRepository<ENTITY> searchRepository, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.repository = repository;
        this.searchRepository = searchRepository;
    }

    /**
     * Creates a provider by resolving the entity class automatically from the generic type
     * hierarchy via reflection, with default lifecycle events (no-op).
     *
     * @param repository       the Spring Data repository for persistence operations
     * @param searchRepository the search and count adapter
     */
    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, SearchRepository<ENTITY> searchRepository) {
        this(repository, searchRepository, EntityCrudEvents.getDefault());
    }

    /**
     * Creates a provider by resolving the entity class via reflection, from a repository
     * that implements both {@link CrudRepository} and {@link SearchRepository}.
     *
     * @param repository the repository implementing both persistence and search
     * @param events     the CRUD lifecycle events handler
     */
    public <R extends CrudRepository<ENTITY, ID> & SearchRepository<ENTITY>> SpringRepositoryCrudProvider(R repository, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        this(repository, repository, events);
    }

    /**
     * Creates a provider by resolving the entity class via reflection, from a repository
     * that implements both {@link CrudRepository} and {@link SearchRepository},
     * with default lifecycle events.
     *
     * @param repository the repository implementing both persistence and search
     */
    public <R extends CrudRepository<ENTITY, ID> & SearchRepository<ENTITY>> SpringRepositoryCrudProvider(R repository) {
        this(repository, repository, EntityCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundEntityException(entityClass, id, "Entity not found with id: " + id));
    }

    @Override
    protected Page<ENTITY> internalPage(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        return SpringToPeluwareAdapters.applyAsPage(pagination, sort, pageable -> searchRepository.findAllBySearch(
            search,
            query,
            pageable
        ));
    }

    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return searchRepository.countBySearch(search, query);
    }

    @Override
    protected boolean internalExists(ID id) {
        return repository.existsById(id);
    }

    @Override
    protected ENTITY internalCreate(ENTITY entity) {
        return repository.save(entity);
    }

    @Override
    protected ENTITY internalUpdate(ENTITY entity) {
        return repository.save(entity);
    }

    @Override
    protected void internalDelete(ENTITY entity) {
        repository.delete(entity);
    }
}
