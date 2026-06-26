package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.Map;
import java.util.function.BiFunction;

public class JpaQueryHelpers {

    private JpaQueryHelpers() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Sets up a Criteria API query context and delegates full configuration and execution
     * to the given callback.
     *
     * <p>Use this overload when you need more than a simple {@code WHERE} clause —
     * for example, a custom {@code SELECT}, {@code ORDER BY}, joins, or subqueries:</p>
     *
     * <pre>{@code
     * return JpaHelpers.query(em, entityClass, String.class, (cb, cq, root) -> {
     *     cq.select(root.get("name"))
     *       .where(cb.equal(root.get("active"), true))
     *       .orderBy(cb.asc(root.get("name")));
     *     return em.createQuery(cq).getResultList();
     * });
     * }</pre>
     *
     * <p>For simple {@code WHERE + executor} queries, prefer
     * {@link #query(EntityManager, Class, Class, BiFunction, JpaCriteriaExecutor)} instead.</p>
     *
     * @param em          the entity manager
     * @param entityClass the entity class to use as the query root
     * @param resultClass the expected result type of the query
     * @param callback    receives {@code cb}, {@code cq}, and {@code root} to configure and execute the query
     * @param <ENTITY>    the entity type
     * @param <RESULT>    the result row type
     * @param <RETURN>    the final return type
     * @return the value returned by {@code callback}
     */
    public static <ENTITY, RESULT, RETURN> RETURN query(
        EntityManager em,
        Class<ENTITY> entityClass,
        Class<RESULT> resultClass,
        JpaCriteriaCallback<ENTITY, RESULT, RETURN> callback
    ) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(resultClass);
        var root = cq.from(entityClass);
        return callback.apply(cb, cq, root);
    }

    /**
     * Sets up a Criteria API query context, applies a predicate, and delegates execution
     * to the given {@link JpaCriteriaExecutor}.
     *
     * <p>Use this overload when the only configuration needed is a {@code WHERE} clause:</p>
     *
     * <pre>{@code
     * return JpaHelpers.query(em, entityClass, Long.class,
     *     (root, cb) -> cb.equal(root.get("id"), id),
     *     JpaCriteriaExecutor.exists()
     * );
     * }</pre>
     *
     * @param em              the entity manager
     * @param entityClass     the entity class to use as the query root
     * @param resultClass     the expected result type of the query
     * @param predicateLoader builds the {@code WHERE} predicate given the root and criteria builder
     * @param executor        configures and executes the query
     * @param <ENTITY>        the entity type
     * @param <RESULT>        the result row type
     * @param <RETURN>        the final return type
     * @return the query result
     */
    public static <ENTITY, RESULT, RETURN> RETURN query(
        EntityManager em,
        Class<ENTITY> entityClass,
        Class<RESULT> resultClass,
        BiFunction<Root<ENTITY>, CriteriaBuilder, Predicate> predicateLoader,
        JpaCriteriaExecutor<ENTITY, RESULT, RETURN> executor
    ) {
        return query(em, entityClass, resultClass, (cb, cq, root) -> {
            cq.where(predicateLoader.apply(root, cb));
            return executor.exec(cq, root, em);
        });
    }

    /**
     * Variant of {@link #query(EntityManager, Class, Class, BiFunction, JpaCriteriaExecutor)}
     * that forwards JPA query hints to the executor.
     *
     * @param em              the entity manager
     * @param entityClass     the entity class to use as the query root
     * @param resultClass     the expected result type of the query
     * @param predicateLoader builds the {@code WHERE} predicate given the root and criteria builder
     * @param executor        configures and executes the query
     * @param hints           JPA query hints passed to {@link JpaCriteriaExecutor#exec}
     * @param <ENTITY>        the entity type
     * @param <RESULT>        the result row type
     * @param <RETURN>        the final return type
     * @return the query result
     */
    public static <ENTITY, RESULT, RETURN> RETURN query(
        EntityManager em,
        Class<ENTITY> entityClass,
        Class<RESULT> resultClass,
        BiFunction<Root<ENTITY>, CriteriaBuilder, Predicate> predicateLoader,
        JpaCriteriaExecutor<ENTITY, RESULT, RETURN> executor,
        Map<String, Object> hints
    ) {
        return query(em, entityClass, resultClass, (cb, cq, root) -> {
            cq.where(predicateLoader.apply(root, cb));
            return executor.exec(cq, root, em, hints);
        });
    }
}
