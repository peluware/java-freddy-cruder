package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.omnisearch.OmniSearch;
import com.peluware.omnisearch.OmniSearchOptions;
import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.CrudRepository;

public abstract class ServiceCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final CrudRepository<@NonNull ENTITY, @NonNull ID> repository;
    protected final OmniSearch omniSearch;

    public ServiceCrudProvider(CrudRepository<@NonNull ENTITY, @NonNull ID> repository, OmniSearch omniSearch, Class<ENTITY> entityClass) {
        super(entityClass);
        this.repository = repository;
        this.omniSearch = omniSearch;
    }

    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundEntityException(entityClass, "Entity not found with id: " + id));
    }

    @Override
    protected Page<ENTITY> internalPage(Pagination pagination, Sort sort) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .pagination(pagination)
                .sort(sort));
    }

    @Override
    protected Page<ENTITY> internalSearch(String search, Pagination pagination, Sort sort) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort));
    }

    @Override
    protected Page<ENTITY> internalSearch(String search, Pagination pagination, Sort sort, String query) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .search(search)
                .query(query)
                .pagination(pagination)
                .sort(sort));
    }

    @Override
    protected long internalCount(String search, String query) {
        return omniSearch.count(entityClass, new OmniSearchOptions()
                .search(search)
                .query(query));
    }

    @Override
    protected long internalCount(String search) {
        return omniSearch.count(entityClass, new OmniSearchOptions()
                .search(search));
    }

    @Override
    protected long internalCount() {
        return repository.count();
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
