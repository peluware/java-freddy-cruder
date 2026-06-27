package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.OwnedPageProvider;
import com.peluware.freddy.cruder.PageProvider;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.*;

import java.util.function.BiFunction;

public final class PeluwareToSpringAdapters {

    private PeluwareToSpringAdapters() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Sort toSort(com.peluware.domain.Sort sort) {
        if (!sort.isSorted()) {
            return Sort.unsorted();
        }
        var orders = sort.orders().stream()
            .map(order -> new Sort.Order(
                order.direction() == com.peluware.domain.Order.Direction.ASC
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC,
                order.property()))
            .toList();
        return Sort.by(orders);
    }

    public static Pageable toPageable(com.peluware.domain.Pagination pagination, com.peluware.domain.Sort sort) {
        if (!pagination.isPaginated()) {
            return Pageable.unpaged();
        }
        return PageRequest.of(pagination.getNumber(), pagination.getSize(), toSort(sort));
    }

    public static <R> Page<R> toPage(Pageable pageable, com.peluware.domain.Page<R> page) {
        if (page instanceof SpringPage<R> sp) return sp.unwrap();
        return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
    }

    public static <R> R apply(Pageable pageable, BiFunction<com.peluware.domain.Pagination, com.peluware.domain.Sort, R> query) {
        var pagination = SpringToPeluwareAdapters.toPagination(pageable);
        var sort = SpringToPeluwareAdapters.toSort(pageable.getSort());
        return query.apply(pagination, sort);
    }

    public static <R> Page<R> applyAsPage(Pageable pageable, BiFunction<com.peluware.domain.Pagination, com.peluware.domain.Sort, com.peluware.domain.Page<R>> query) {
        return toPage(pageable, apply(pageable, query));
    }

    public static <OUTPUT> Page<OUTPUT> page(
        PageProvider<OUTPUT> provider,
        @Nullable String search,
        @Nullable String query,
        Pageable pageable
    ) {
        return applyAsPage(pageable, (pagination, sort) -> provider.page(search, query, pagination, sort));
    }

    public static <OWNER_ID, OUTPUT> Page<OUTPUT> page(
        OwnedPageProvider<OWNER_ID, OUTPUT> provider,
        OWNER_ID ownerId,
        @Nullable String search,
        @Nullable String query,
        Pageable pageable
    ) {
        return applyAsPage(pageable, (pagination, sort) -> provider.page(ownerId, search, query, pagination, sort));
    }
}
