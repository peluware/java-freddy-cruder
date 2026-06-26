package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.omnisearch.jpa.JpaUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@FunctionalInterface
public interface JpaCriteriaExecutor<SELECTED, RESULT, RETURN> {

    RETURN exec(CriteriaQuery<RESULT> cq, Path<SELECTED> path, EntityManager entityManager, Map<String, Object> hints);

    default RETURN exec(CriteriaQuery<RESULT> cq, Path<SELECTED> path, EntityManager entityManager) {
        return exec(cq, path, entityManager, Map.of());
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, List<SELECTED>> list(Sort sort, Pagination pagination) {
        return (cq, root, em, hints) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            cq.select(root);

            if (sort.isSorted()) {
                cq.orderBy(JpaUtils.getOrders(sort, root, cb, em.getMetamodel()));
            }

            TypedQuery<SELECTED> query = createTypedQuery(cq, em, hints);

            if (pagination.isPaginated()) {
                query
                    .setFirstResult(pagination.getNumber() * pagination.getSize())
                    .setMaxResults(pagination.getSize());
            }

            return query.getResultList();
        };
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, List<SELECTED>> list(Sort sort) {
        return list(sort, Pagination.unpaginated());
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, List<SELECTED>> list(Pagination pagination) {
        return list(Sort.unsorted(), pagination);
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, List<SELECTED>> list() {
        return list(Sort.unsorted(), Pagination.unpaginated());
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, Long, Long> count() {
        return (cq, root, em, hints) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            cq.select(cb.count(root));

            TypedQuery<Long> query = createTypedQuery(cq, em, hints);

            return query.getSingleResult();
        };
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, Long, Boolean> exists() {
        return (cq, _, em, hints) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            cq.select(cb.literal(1L));               // SELECT 1, no count, no entidad

            TypedQuery<Long> query = createTypedQuery(cq, em, hints);

            return !query
                .setMaxResults(1)                    // LIMIT 1 → short-circuit
                .getResultList()
                .isEmpty();
        };
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, Optional<SELECTED>> first() {
        return (cq, root, em, hints) -> {
            cq.select(root);

            TypedQuery<SELECTED> query = createTypedQuery(cq, em, hints);

            return query
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
        };
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, SELECTED> one() {
        return (cq, path, em, hints) -> {
            cq.select(path);

            TypedQuery<SELECTED> query = createTypedQuery(cq, em, hints);

            return query
                .getSingleResult();
        };
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, Stream<SELECTED>> stream() {
        return (cq, path, em, hints) -> {
            cq.select(path);

            TypedQuery<SELECTED> query = createTypedQuery(cq, em, hints);

            return query
                .getResultStream();
        };
    }

    private static <T> TypedQuery<T> createTypedQuery(CriteriaQuery<T> cq, EntityManager em, Map<String, Object> hints) {
        TypedQuery<T> query = em.createQuery(cq);
        hints.forEach(query::setHint);
        return query;
    }
}
