package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Order;
import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import org.springframework.data.util.Streamable;

import java.util.List;
import java.util.function.Function;

public final class SpringToPeluwareAdapters {

    private SpringToPeluwareAdapters() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Pagination toPagination(org.springframework.data.domain.Pageable pageable) {
        return pageable.isUnpaged()
            ? Pagination.unpaginated()
            : Pagination.of(pageable.getPageNumber(), pageable.getPageSize());
    }

    public static Sort toSort(org.springframework.data.domain.Sort sort) {
        return sort.isUnsorted()
            ? Sort.unsorted()
            : new Sort(toOrders(sort));
    }

    public static List<Order> toOrders(Streamable<org.springframework.data.domain.Sort.Order> sort) {
        return sort.stream()
            .map(order -> new Order(
                order.getProperty(),
                order.isAscending()
                    ? Order.Direction.ASC
                    : Order.Direction.DESC))
            .toList();
    }

    public static <T> Page<T> toPage(org.springframework.data.domain.Page<T> springPage, Pagination pagination, Sort sort) {
        return new SpringPage<>(springPage, pagination, sort);
    }

    public static <T> Page<T> applyAsPage(Pagination pagination, Sort sort, Function<org.springframework.data.domain.Pageable, org.springframework.data.domain.Page<T>> query) {
        var pageable = PeluwareToSpringAdapters.toPageable(pagination, sort);
        return toPage(query.apply(pageable), pagination, sort);
    }
}
