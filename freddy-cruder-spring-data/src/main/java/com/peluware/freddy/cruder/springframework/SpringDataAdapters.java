package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.domain.Order;
import com.peluware.freddy.cruder.CrudOptions;
import com.peluware.freddy.cruder.CrudProvider;
import com.peluware.freddy.cruder.OwnedCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;

import java.util.List;

public final class SpringDataAdapters {

    private SpringDataAdapters() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Bridges a {@link CrudProvider#page} call into a Spring {@link Page}.
     *
     * <p>
     * Converts the Spring {@link Pageable} into {@link Pagination} and {@link Sort},
     * delegates to the provider, and wraps the result in a {@link PageImpl}.
     * </p>
     *
     * <p>
     * This method is intended for use within a Spring controller method, allowing
     * seamless integration of the provider's pagination and sorting capabilities
     * with Spring's data handling abstractions.
     * </p>
     *
     * @param provider the CRUD provider to delegate to
     * @param search   optional text-based search (maybe {@code null})
     * @param query  optional additional filtering expression (maybe {@code null})
     * @param pageable Spring pagination and sorting abstraction (must not be {@code null})
     * @param options execution modifiers that may alter fetching, filtering or performance behavior
     * @param <ID>      type of the resource identifier
     * @param <OUTPUT> type of the output DTO
     * @return a Spring {@link Page} containing the paginated result set
     */
    public static <ID, OUTPUT> Page<@NonNull OUTPUT> toSpringDataCall(
            CrudProvider<ID, ?, OUTPUT> provider,
            String search,
            String query,
            Pageable pageable,
            CrudOptions options
    ) {
        var pagination = toPagination(pageable);
        var sort = toSort(pageable.getSort());
        var page = provider.page(search, query, pagination, sort, options);
        return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
    }

    /**
     * Bridges a {@link OwnedCrudProvider#page} call into a Spring {@link Page},
     * scoped to the given owner.
     *
     * <p>
     * Converts the Spring {@link Pageable} into {@link Pagination} and {@link Sort},
     * delegates to the provider, and wraps the result in a {@link PageImpl}.
     * </p>
     *
     * @param provider   the owned CRUD provider
     * @param ownerId    unique identifier of the owning resource
     * @param search     optional text-based search
     * @param query      optional additional filtering expression
     * @param pageable   Spring pagination and sorting abstraction
     * @param options    execution modifiers
     * @param <OWNER_ID> type of the owner identifier
     * @param <ID>       type of the resource identifier
     * @param <OUTPUT>   type of the output DTO
     * @return a Spring {@link Page} scoped to the owner
     */
    public static <OWNER_ID, ID, OUTPUT> Page<@NonNull OUTPUT> toSpringOwnedDataCall(
            OwnedCrudProvider<OWNER_ID, ID, ?, OUTPUT> provider,
            OWNER_ID ownerId,
            String search,
            String query,
            Pageable pageable,
            CrudOptions options
    ) throws com.peluware.freddy.cruder.NotFoundException {
        var pagination = toPagination(pageable);
        var sort = toSort(pageable.getSort());
        var page = provider.page(ownerId, search, query, pagination, sort, options);
        return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
    }

    public static Pagination toPagination(Pageable pageable) {
        return pageable.isUnpaged()
                ? Pagination.unpaginated()
                : Pagination.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    public static Sort toSort(org.springframework.data.domain.Sort sort) {
        return sort.isUnsorted()
                ? Sort.unsorted()
                : new Sort(toOrders(sort));
    }

    private static List<Order> toOrders(Streamable<org.springframework.data.domain.Sort.Order> sort) {
        return sort.stream()
                .map(order -> new Order(
                        order.getProperty(),
                        order.isAscending()
                                ? Order.Direction.ASC
                                : Order.Direction.DESC))
                .toList();
    }
}
