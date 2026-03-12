package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.domain.Order;
import com.peluware.freddy.cruder.CrudProvider;
import com.peluware.freddy.cruder.OwnedCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.function.BiFunction;

public final class SpringDataAdapters {

    private SpringDataAdapters() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <R> R withSpringPageable(Pageable pageable, BiFunction<Pagination, Sort, R> providerCall) {
        var pagination = toPeluwarePagination(pageable);
        var sort = toPeluwareSort(pageable.getSort());
        return providerCall.apply(pagination, sort);
    }


    public static <ID, OUTPUT> Page<@NonNull OUTPUT> page(
            CrudProvider<ID, ?, OUTPUT> provider,
            String search,
            String query,
            Pageable pageable
    ) {
        return withSpringPageable(pageable, (pagination, sort) -> {
            var page = provider.page(search, query, pagination, sort);
            return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
        });
    }

    public static <OWNER_ID, ID, OUTPUT> Page<@NonNull OUTPUT> page(
            OwnedCrudProvider<OWNER_ID, ID, ?, OUTPUT> provider,
            OWNER_ID ownerId,
            String search,
            String query,
            Pageable pageable
    ) {
        return withSpringPageable(pageable, (pagination, sort) -> {
            var page = provider.page(ownerId, search, query, pagination, sort);
            return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
        });
    }

    public static Pagination toPeluwarePagination(Pageable pageable) {
        return pageable.isUnpaged()
                ? Pagination.unpaginated()
                : Pagination.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    public static Sort toPeluwareSort(org.springframework.data.domain.Sort sort) {
        return sort.isUnsorted()
                ? Sort.unsorted()
                : new Sort(toPeluwareOrders(sort));
    }

    public static List<Order> toPeluwareOrders(Streamable<org.springframework.data.domain.Sort.Order> sort) {
        return sort.stream()
                .map(order -> new Order(
                        order.getProperty(),
                        order.isAscending()
                                ? Order.Direction.ASC
                                : Order.Direction.DESC))
                .toList();
    }

    public static org.springframework.data.domain.Sort toSpringSort(Sort sort) {
        if (!sort.isSorted()) {
            return org.springframework.data.domain.Sort.unsorted();
        }
        var orders = sort.orders().stream()
                .map(order -> new org.springframework.data.domain.Sort.Order(
                        order.direction() == Order.Direction.ASC
                                ? org.springframework.data.domain.Sort.Direction.ASC
                                : org.springframework.data.domain.Sort.Direction.DESC,
                        order.property()))
                .toList();
        return org.springframework.data.domain.Sort.by(orders);
    }

    public static Pageable toSpringPageable(Pagination pagination, Sort sort) {

        if (!pagination.isPaginated()) {
            return Pageable.unpaged();
        }

        var springSort = toSpringSort(sort);
        return org.springframework.data.domain.PageRequest.of(
                pagination.getNumber(),
                pagination.getSize(),
                springSort
        );
    }
}
