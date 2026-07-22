package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

/**
 * Creates the {@link CriteriaQuery} the pipeline builds on. {@link #of(Class)} covers the common
 * case; {@link #tuple()} and {@link #untyped()} cover the multi-select and untyped forms.
 *
 * @param <RESULT> the result type of the criteria query
 */
@FunctionalInterface
public interface JpaCriteria<RESULT> {

    CriteriaQuery<RESULT> create(CriteriaBuilder cb);

    /**
     * A query with the given result type — {@code cb.createQuery(resultClass)}.
     *
     * @param resultClass the result type
     * @param <R>         the result type
     * @return a criteria for {@code resultClass}
     */
    static <R> JpaCriteria<R> of(Class<R> resultClass) {
        return cb -> cb.createQuery(resultClass);
    }

    /**
     * A {@link Tuple} query — {@code cb.createTupleQuery()}.
     *
     * @return a tuple criteria
     */
    static JpaCriteria<Tuple> tuple() {
        return CriteriaBuilder::createTupleQuery;
    }

    /**
     * An untyped query — {@code cb.createQuery()}.
     *
     * @return an untyped criteria
     */
    static JpaCriteria<Object> untyped() {
        return CriteriaBuilder::createQuery;
    }
}
