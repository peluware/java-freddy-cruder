package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.metamodel.Metamodel;

import java.util.List;

/**
 * A complete query as a single object: the {@link JpaSource source}, {@link JpaSelection selection},
 * {@link JpaPredicate filter}, {@link JpaOrder ordering}, and {@link JpaResult result} in one place.
 * Implement it to package a reusable query — a count, a search, a projection — as a named class,
 * then build and run it, wrapping it in {@link JpaQueryHints} to set query hints:
 *
 * <pre>{@code
 * var product = new JpaQueryHints<>(
 *         new EntityFindQuery<>(Product.class, byId, onMissing),
 *         JpaHints.fetchGraph(graph))
 *     .exec(entityManager);
 * }</pre>
 *
 * <p>{@link #orders} defaults to no ordering, so only queries that sort need to implement it.</p>
 *
 * @param <SELECTED> the entity type of the query source
 * @param <RESULT>   the row type produced by the selection
 * @param <RETURN>   the value produced by the result
 */
public interface JpaQuery<SELECTED, RESULT, RETURN>
    extends JpaCriteria<RESULT>,
            JpaSource<SELECTED, RESULT>,
            JpaSelection<SELECTED, RESULT>,
            JpaPredicate<SELECTED>,
            JpaGroupBy<SELECTED>,
            JpaOrder<SELECTED>,
            JpaResult<RESULT, RETURN> {

    @Override
    default List<Expression<?>> groupBy(From<?, SELECTED> from, CriteriaBuilder cb) {
        return List.of();
    }

    @Override
    default List<Order> orders(From<?, SELECTED> from, CriteriaBuilder cb, Metamodel metamodel) {
        return List.of();
    }

    /**
     * Runs this query on the given entity manager. Equivalent to
     * {@link JpaQueryExecutor#exec(EntityManager, JpaQuery)}, but keeps the call site linear
     * instead of nesting the query inside the executor call.
     *
     * @param entityManager the entity manager to run the query on
     * @return the value produced by the result
     */
    default RETURN exec(EntityManager entityManager) {
        return JpaQueryExecutor.exec(entityManager, this);
    }
}
