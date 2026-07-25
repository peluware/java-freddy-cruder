package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * A complete query as a single object: the {@link JpaSource source}, {@link JpaSelection selection},
 * {@link JpaPredicate filter}, {@link JpaOrder ordering}, and {@link JpaResult result} in one place.
 * Implement it to package a reusable query — a count, a search, a projection — as a named class,
 * then build, configure and run it in one chain:
 *
 * <pre>{@code
 * var product = new EntityFindQuery<>(Product.class, byId, onMissing)
 *     .addHints(JpaHints.fetchGraph(graph))
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
     * Returns the same query with the given hints applied to its result, so it can still be run
     * as a whole via {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}.
     *
     * @param hints the JPA query hints to apply
     * @return a query that applies the hints before materializing
     */
    @Override
    default JpaQuery<SELECTED, RESULT, RETURN> addHints(Map<String, Object> hints) {
        var self = this;
        return new JpaQuery<>() {
            @Override
            public CriteriaQuery<RESULT> create(CriteriaBuilder cb) {
                return self.create(cb);
            }

            @Override
            public From<?, SELECTED> from(CriteriaQuery<RESULT> cq) {
                return self.from(cq);
            }

            @Override
            public Selection<? extends RESULT> select(From<?, SELECTED> from, CriteriaBuilder cb) {
                return self.select(from, cb);
            }

            @Override
            public @Nullable Predicate build(From<?, SELECTED> from, CriteriaBuilder cb) {
                return self.build(from, cb);
            }

            @Override
            public List<Expression<?>> groupBy(From<?, SELECTED> from, CriteriaBuilder cb) {
                return self.groupBy(from, cb);
            }

            @Override
            public List<Order> orders(From<?, SELECTED> from, CriteriaBuilder cb, Metamodel metamodel) {
                return self.orders(from, cb, metamodel);
            }

            @Override
            public RETURN result(TypedQuery<RESULT> query) {
                hints.forEach(query::setHint);
                return self.result(query);
            }
        };
    }

    /**
     * Runs this query on the given entity manager — the terminal step of the
     * {@code new … → addHints → exec} chain. Equivalent to
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
