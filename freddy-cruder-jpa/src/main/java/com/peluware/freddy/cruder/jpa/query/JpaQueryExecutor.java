package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;

import java.util.List;

/**
 * Runs a Criteria API query built from a {@link JpaSource source}, {@link JpaSelection selection},
 * {@link JpaPredicate filter}, and {@link JpaOrder ordering}, and materializes it with a
 * {@link JpaResult}.
 */
public final class JpaQueryExecutor {

    private JpaQueryExecutor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Composes and runs a query from its axes plus a result strategy.
     *
     * @param em         the entity manager
     * @param criteria   creates the criteria query for the result type
     * @param source     builds the query source
     * @param selection  builds the {@code SELECT} clause
     * @param predicate  builds the {@code WHERE} predicate
     * @param order      builds the {@code ORDER BY} clause
     * @param result     executes the query and materializes the return value
     * @return the value produced by {@code result}
     */
    public static <SELECTED, RESULT, RETURN> RETURN exec(
        EntityManager em,
        JpaCriteria<RESULT> criteria,
        JpaSource<SELECTED, RESULT> source,
        JpaSelection<SELECTED, RESULT> selection,
        JpaPredicate<SELECTED> predicate,
        JpaOrder<SELECTED> order,
        JpaResult<RESULT, RETURN> result
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RESULT> cq = criteria.create(cb);

        From<?, SELECTED> from = source.from(cq);

        cq.select(selection.select(from, cb));

        Predicate where = predicate.build(from, cb);
        if (where != null) {
            cq.where(where);
        }

        List<Order> orders = order.orders(from, cb, em.getMetamodel());
        if (!orders.isEmpty()) {
            cq.orderBy(orders);
        }

        TypedQuery<RESULT> query = em.createQuery(cq);

        return result.result(query);
    }

    /**
     * Composes and runs a query for the given result type.
     */
    public static <SELECTED, RESULT, RETURN> RETURN exec(
        EntityManager em,
        Class<RESULT> resultClass,
        JpaSource<SELECTED, RESULT> source,
        JpaSelection<SELECTED, RESULT> selection,
        JpaPredicate<SELECTED> predicate,
        JpaOrder<SELECTED> order,
        JpaResult<RESULT, RETURN> result
    ) {
        return exec(em, JpaCriteria.of(resultClass), source, selection, predicate, order, result);
    }

    /**
     * Composes and runs a query with no ordering.
     */
    public static <SELECTED, RESULT, RETURN> RETURN exec(
        EntityManager em,
        Class<RESULT> resultClass,
        JpaSource<SELECTED, RESULT> source,
        JpaSelection<SELECTED, RESULT> selection,
        JpaPredicate<SELECTED> predicate,
        JpaResult<RESULT, RETURN> result
    ) {
        return exec(em, resultClass, source, selection, predicate, JpaOrder.unsorted(), result);
    }

    /**
     * Runs a {@link JpaQuery} — a single object providing every axis, including its own result type.
     *
     * @param em    the entity manager
     * @param query the query, supplying the criteria, source, selection, filter, ordering, and result
     * @return the value produced by the query's {@link JpaResult}
     */
    public static <SELECTED, RESULT, RETURN> RETURN exec(
        EntityManager em,
        JpaQuery<SELECTED, RESULT, RETURN> query
    ) {
        return exec(em, query, query, query, query, query, query);
    }

    // ------------------------------------------------------------
    // CONVENIENCE — root source ({@code cq.from(entityClass)})
    // ------------------------------------------------------------

    /**
     * Projected query rooted at {@code entityClass} (no join), ordered.
     */
    public static <SELECTED, RESULT, RETURN> RETURN exec(
        EntityManager em,
        Class<SELECTED> entityClass,
        Class<RESULT> resultClass,
        JpaSelection<SELECTED, RESULT> selection,
        JpaPredicate<SELECTED> predicate,
        JpaOrder<SELECTED> order,
        JpaResult<RESULT, RETURN> result
    ) {
        return exec(em, resultClass, JpaSource.root(entityClass), selection, predicate, order, result);
    }

    /**
     * Projected query rooted at {@code entityClass} (no join), unordered.
     */
    public static <SELECTED, RESULT, RETURN> RETURN exec(
        EntityManager em,
        Class<SELECTED> entityClass,
        Class<RESULT> resultClass,
        JpaSelection<SELECTED, RESULT> selection,
        JpaPredicate<SELECTED> predicate,
        JpaResult<RESULT, RETURN> result
    ) {
        return exec(em, entityClass, resultClass, selection, predicate, JpaOrder.unsorted(), result);
    }

    // ------------------------------------------------------------
    // CONVENIENCE — root source, entity result (no selection)
    // ------------------------------------------------------------

    /**
     * Query rooted at {@code entityClass} returning whole entities, ordered.
     */
    public static <ENTITY, RETURN> RETURN exec(
        EntityManager em,
        Class<ENTITY> entityClass,
        JpaPredicate<ENTITY> predicate,
        JpaOrder<ENTITY> order,
        JpaResult<ENTITY, RETURN> result
    ) {
        return exec(em, entityClass, entityClass, JpaSelection.self(), predicate, order, result);
    }

    /**
     * Query rooted at {@code entityClass} returning whole entities, unordered.
     */
    public static <ENTITY, RETURN> RETURN exec(
        EntityManager em,
        Class<ENTITY> entityClass,
        JpaPredicate<ENTITY> predicate,
        JpaResult<ENTITY, RETURN> result
    ) {
        return exec(em, entityClass, predicate, JpaOrder.unsorted(), result);
    }
}
