package com.peluware.freddy.cruder.reactive.mutiny;


import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.CrudOperation;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.reactive.mutiny.events.MutinyReadEvents;
import com.peluware.freddy.cruder.utils.StringUtils;
import io.smallrye.mutiny.Uni;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Support interface for reactive read operations, providing default behaviors
 * such as event firing, search normalization, and reactive pre/post processing hooks.
 *
 * @param <E>  Entity type
 * @param <ID> Identifier type
 */
public interface MutinyReadProviderSupport<E, ID> extends MutinyReadProvider<E, ID>, MutinyCrudProcessSupport {

    /**
     * {@inheritDoc}
     */
    @Override
    default Uni<Page<E>> page(String search, Pagination pagination, Sort sort, String query) {
        var events = getEvents();
        return preProcess(CrudOperation.PAGE)
                .onItem().transformToUni(v -> resolvePage(StringUtils.normalize(search), pagination, sort, query))
                .onItem().call(events::onPage)
                .onItem().invoke(page -> page.getContent().forEach(events::eachEntity))
                .onItem().call(page -> postProcess(CrudOperation.PAGE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Uni<E> find(@NotNull ID id) {
        var events = getEvents();
        return preProcess(CrudOperation.FIND)
                .onItem().transformToUni(v -> internalFind(id))
                .onItem().call(events::onFind)
                .onItem().invoke(events::eachEntity)
                .onItem().call(v -> postProcess(CrudOperation.FIND));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Uni<List<E>> find(@NotNull @NotEmpty List<ID> ids) {
        var events = getEvents();
        return preProcess(CrudOperation.FIND)
                .onItem().transformToUni(v -> internalFind(ids))
                .onItem().call(entities -> events.onFind(entities, ids))
                .onItem().invoke(list -> list.forEach(events::eachEntity))
                .onItem().call(v -> postProcess(CrudOperation.FIND));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Uni<Long> count(String search, String query) {
        var events = getEvents();
        return preProcess(CrudOperation.COUNT)
                .onItem().transformToUni(v -> resolveCount(StringUtils.normalize(search), query))
                .onItem().call(events::onCount)
                .onItem().call(v -> postProcess(CrudOperation.COUNT));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default Uni<Boolean> exists(@NotNull ID id) {
        var events = getEvents();
        return preProcess(CrudOperation.EXISTS)
                .onItem().transformToUni(v -> internalExists(id))
                .onItem().call(exists -> events.onExists(exists, id))
                .onItem().call(exists -> postProcess(CrudOperation.EXISTS));
    }

    @Protected
    default Uni<Page<E>> resolvePage(String search, Pagination pagination, Sort sort, String query) {
        if (pagination == null) pagination = Pagination.unpaginated();
        if (sort == null) sort = Sort.unsorted();
        if (query == null) {
            return StringUtils.isEmpty(search)
                    ? internalPage(pagination, sort)
                    : internalSearch(search, pagination, sort);
        }
        return internalSearch(search, pagination, sort, query);
    }

    @Protected
    default Uni<Long> resolveCount(String search, String query) {
        if (query == null) {
            return StringUtils.isEmpty(search)
                    ? internalCount()
                    : internalCount(search);
        }
        return internalCount(search, query);
    }


    /**
     * Factory method for creating the {@link MutinyReadEvents} instance.
     * <p>
     * Subclasses can override to provide custom event handling.
     * </p>
     */
    @Protected
    default MutinyReadEvents<E, ID> getEvents() {
        return MutinyReadEvents.getDefault();
    }

    @Protected
    Uni<Page<E>> internalPage(Pagination pagination, Sort sort);

    @Protected
    Uni<Page<E>> internalSearch(String search, Pagination pagination, Sort sort);

    @Protected
    Uni<Page<E>> internalSearch(String search, Pagination pagination, Sort sort, String query);

    @Protected
    Uni<E> internalFind(ID id);

    @Protected
    Uni<List<E>> internalFind(List<ID> ids);

    @Protected
    Uni<Long> internalCount(String search, String query);

    @Protected
    Uni<Long> internalCount(String search);

    @Protected
    Uni<Long> internalCount();

    @Protected
    Uni<Boolean> internalExists(ID id);
}

