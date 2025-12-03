package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.domain.Order;
import com.peluware.freddy.cruder.CrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

public final class CrudProviderUtils {

    private CrudProviderUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <ID, OUTPUT> Page<@NonNull OUTPUT> adaptSpringPage(CrudProvider<ID, ?, OUTPUT> provider,
                                                                     String search,
                                                                     String query,
                                                                     Pageable pageable) {
        var pagination = toPagination(pageable);
        var sort = toSort(pageable.getSort());
        var page = provider.page(search, query, pagination, sort);
        return new PageImpl<>(page.getContent(), pageable, page.getTotalElements());
    }

    public static Pagination toPagination(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return Pagination.unpaginated();
        } else {
            return Pagination.of(pageable.getPageNumber(), pageable.getPageSize());
        }
    }

    public static Sort toSort(org.springframework.data.domain.Sort sort) {
        if (sort.isUnsorted()) {
            return Sort.unsorted();
        } else {
            return new Sort(sort.stream()
                    .map(order -> new Order(
                            order.getProperty(),
                            order.isAscending()
                                    ? Order.Direction.ASC
                                    : Order.Direction.DESC))
                    .toList());
        }
    }
}
