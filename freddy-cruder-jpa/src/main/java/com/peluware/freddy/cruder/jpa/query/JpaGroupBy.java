package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;

import java.util.List;

/**
 * Builds the {@code GROUP BY} clause of a Criteria query — a list of grouping expressions, applied
 * by the executor after the {@code SELECT} clause. Use {@link #none()} for no grouping.
 *
 * @param <SELECTED> the entity type of the query source
 */
@FunctionalInterface
public interface JpaGroupBy<SELECTED> {

    List<Expression<?>> groupBy(From<?, SELECTED> from, CriteriaBuilder cb);

    /**
     * No grouping.
     *
     * @param <SELECTED> the entity type of the query source
     * @return a grouping that contributes no {@code GROUP BY}
     */
    static <SELECTED> JpaGroupBy<SELECTED> none() {
        return (_, _) -> List.of();
    }

    /**
     * Groups by the query source itself — the equivalent of {@code GROUP BY from}. Lets a
     * projection select any other column of {@code from} freely alongside an aggregate over a
     * joined plural association, without listing each one individually.
     *
     * @param <SELECTED> the entity type of the query source
     * @return a grouping that groups by {@code from} as a whole
     */
    static <SELECTED> JpaGroupBy<SELECTED> self() {
        return (from, _) -> List.of(from);
    }
}
