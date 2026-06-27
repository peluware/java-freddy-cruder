package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;

/**
 * Bridge type that extends Peluware {@link Page} while retaining the original
 * Spring Data {@link org.springframework.data.domain.Page} so it can be unwrapped
 * without re-wrapping when passed back to a Spring layer.
 */
class SpringPage<T> extends Page<T> {

    private final org.springframework.data.domain.Page<T> origin;

    SpringPage(org.springframework.data.domain.Page<T> origin, Pagination pagination, Sort sort) {
        super(origin.getContent(), pagination, sort, origin.getTotalElements());
        this.origin = origin;
    }

    org.springframework.data.domain.Page<T> unwrap() {
        return origin;
    }
}
