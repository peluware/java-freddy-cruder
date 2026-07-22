package com.peluware.freddy.cruder.jpa.query;

import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.jpa.JpaUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.metamodel.Metamodel;

import java.util.List;

/**
 * Builds the {@code ORDER BY} clause of a Criteria query. Build one from a domain {@link Sort}
 * with {@link #by(Sort)}, or {@link #unsorted()} for none.
 *
 * @param <SELECTED> the entity type of the query source
 */
@FunctionalInterface
public interface JpaOrder<SELECTED> {

    List<Order> orders(From<?, SELECTED> from, CriteriaBuilder cb, Metamodel metamodel);

    /**
     * Orders by the given domain {@link Sort}, resolving property paths via the metamodel.
     *
     * @param sort the sort specification
     * @param <SELECTED> the entity type of the query source
     * @return an order that maps {@code sort} to Criteria {@link Order}s
     */
    static <SELECTED> JpaOrder<SELECTED> by(Sort sort) {
        return (from, cb, metamodel) -> JpaUtils.getOrders(sort, from, cb, metamodel);
    }

    /**
     * No ordering.
     *
     * @param <SELECTED> the entity type of the query source
     * @return an order that contributes no {@code ORDER BY}
     */
    static <SELECTED> JpaOrder<SELECTED> unsorted() {
        return (_, _, _) -> List.of();
    }
}
