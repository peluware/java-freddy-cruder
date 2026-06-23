package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.omnisearch.EntityOmniSearch;
import com.peluware.omnisearch.OmniSearch;
import com.peluware.omnisearch.OmniSearchOptions;
import org.jspecify.annotations.Nullable;
import org.springframework.data.repository.CrudRepository;

public abstract class SpringRepositoryCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends SpringEntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final CrudRepository<ENTITY, ID> repository;
    protected final EntityOmniSearch<ENTITY> entityOmniSearch;

    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, EntityOmniSearch<ENTITY> entityOmniSearch, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.repository = repository;
        this.entityOmniSearch = entityOmniSearch;
    }

    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, EntityOmniSearch<ENTITY> entityOmniSearch, Class<ENTITY> entityClass) {
        this(repository, entityOmniSearch, entityClass, EntityCrudEvents.getDefault());
    }

    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, OmniSearch omniSearch, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.repository = repository;
        this.entityOmniSearch = omniSearch.forEntity(entityClass);
    }

    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, OmniSearch omniSearch, Class<ENTITY> entityClass) {
        this(repository, omniSearch, entityClass, EntityCrudEvents.getDefault());
    }

    public SpringRepositoryCrudProvider(CrudSearchRepository<ENTITY, ID> repository, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        this(repository, repository, entityClass, events);
    }

    public SpringRepositoryCrudProvider(CrudSearchRepository<ENTITY, ID> repository, Class<ENTITY> entityClass) {
        this(repository, repository, entityClass, EntityCrudEvents.getDefault());
    }


    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, EntityOmniSearch<ENTITY> entityOmniSearch, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.repository = repository;
        this.entityOmniSearch = entityOmniSearch;
    }

    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, EntityOmniSearch<ENTITY> entityOmniSearch) {
        this(repository, entityOmniSearch, EntityCrudEvents.getDefault());
    }

    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, OmniSearch omniSearch, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.repository = repository;
        this.entityOmniSearch = omniSearch.forEntity(super.entityClass);
    }

    public SpringRepositoryCrudProvider(CrudRepository<ENTITY, ID> repository, OmniSearch omniSearch) {
        this(repository, omniSearch, EntityCrudEvents.getDefault());
    }

    public SpringRepositoryCrudProvider(CrudSearchRepository<ENTITY, ID> repository, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        this(repository, repository, events);
    }

    public SpringRepositoryCrudProvider(CrudSearchRepository<ENTITY, ID> repository) {
        this(repository, repository, EntityCrudEvents.getDefault());
    }

    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        return repository.findById(id)
            .orElseThrow(() -> new NotFoundEntityException(entityClass, id, "Entity not found with id: " + id));
    }

    @Override
    protected Page<ENTITY> internalPage(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        return entityOmniSearch.page(new OmniSearchOptions()
            .search(search)
            .query(query)
            .pagination(pagination)
            .sort(sort)
        );
    }

    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return entityOmniSearch.count(new OmniSearchOptions()
            .search(search)
            .query(query)
        );
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
