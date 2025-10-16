package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.events.ReadEvents;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.freddy.cruder.utils.StringUtils;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Support implementation of {@link ReadProvider} providing common behavior based on default methods.
 * <p>
 * This class resolves default behaviors such as event handling, search normalization,
 * and dispatching calls to specialized internal methods.
 *
 * @param <E>  Entity type
 * @param <ID> Identifier type
 */
public interface ReadProviderSupport<E, ID> extends ReadProvider<E, ID>, CrudProcessSupport {

    /**
     * {@inheritDoc}
     */
    @Override
    default Page<E> page(String search, Pagination pagination, Sort sort, String query) {
        preProcess(CrudOperation.PAGE);

        var normalized = StringUtils.normalize(search);
        var page = resolvePage(normalized, pagination, sort, query);

        var events = getEvents();
        events.onPage(page);
        page.getContent().forEach(events::eachEntity);

        postProcess(CrudOperation.PAGE);
        return page;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default E find(@NotNull ID id) throws NotFoundEntityException {
        preProcess(CrudOperation.FIND);

        var model = internalFind(id);

        var events = getEvents();
        events.onFind(model);
        events.eachEntity(model);

        postProcess(CrudOperation.FIND);
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default List<E> find(@NotNull @NotEmpty List<ID> ids) {
        preProcess(CrudOperation.FIND);

        var list = internalFind(ids);

        var events = getEvents();
        events.onFind(list, ids);
        list.forEach(events::eachEntity);

        postProcess(CrudOperation.FIND);
        return list;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default long count(String search, String query) {
        preProcess(CrudOperation.COUNT);

        var normalized = StringUtils.normalize(search);
        var count = resolveCount(normalized, query);

        var events = getEvents();
        events.onCount(count);

        postProcess(CrudOperation.COUNT);
        return count;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean exists(@NotNull ID id) {
        preProcess(CrudOperation.EXISTS);

        var exists = internalExists(id);

        var events = getEvents();
        events.onExists(exists, id);

        postProcess(CrudOperation.EXISTS);
        return exists;
    }

    private Page<E> resolvePage(String search, Pagination pagination, Sort sort, String query) {
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

    /**
     * Provides the event handler instance for Read operations.
     * <p>
     * Default implementation returns a no-op event handler.
     * Subclasses may override to provide custom event handling logic.
     *
     * @return the event handler instance
     */
    @Protected
    default ReadEvents<E, ID> getEvents() {
        return ReadEvents.getDefault();
    }


    @Protected
    Page<E> internalPage(Pagination pagination, Sort sort);

    @Protected
    Page<E> internalSearch(String search, Pagination pagination, Sort sort);

    @Protected
    Page<E> internalSearch(String search, Pagination pagination, Sort sort, String query);

    @Protected
    E internalFind(ID id) throws NotFoundEntityException;

    @Protected
    List<E> internalFind(List<ID> ids);

    @Protected
    long internalCount(String search, String query);

    @Protected
    long internalCount(String search);

    @Protected
    long internalCount();

    @Protected
    boolean internalExists(ID id);


}
