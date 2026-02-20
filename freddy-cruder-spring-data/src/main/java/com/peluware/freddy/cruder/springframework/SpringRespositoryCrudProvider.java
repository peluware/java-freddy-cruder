package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.omnisearch.EntityOmniSearch;
import com.peluware.omnisearch.OmniSearchOptions;
import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;

public abstract class SpringRespositoryCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends SpringEntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final CrudRepository<@NonNull ENTITY, @NonNull ID> repository;
    protected final EntityOmniSearch<@NonNull ENTITY> entityOmniSearch;

    public SpringRespositoryCrudProvider(CrudRepository<@NonNull ENTITY, @NonNull ID> repository, EntityOmniSearch<ENTITY> entityOmniSearch, Class<ENTITY> entityClass) {
        super(entityClass);
        this.repository = repository;
        this.entityOmniSearch = entityOmniSearch;
    }

    public SpringRespositoryCrudProvider(CrudSearchRepository<@NonNull ENTITY, @NonNull ID> repository, Class<ENTITY> entityClass) {
        this(repository, repository, entityClass);
    }

    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException(entityClass, "Entity not found with id: " + id));
    }

    @Override
    protected Page<ENTITY> internalPage(String search, String query, Pagination pagination, Sort sort) {
        return entityOmniSearch.page(new OmniSearchOptions()
                .search(search)
                .query(query)
                .pagination(pagination)
                .sort(sort)
        );
    }

    @Override
    protected long internalCount(String search, String query) {
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
