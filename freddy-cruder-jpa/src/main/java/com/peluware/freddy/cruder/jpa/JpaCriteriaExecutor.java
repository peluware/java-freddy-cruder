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

@FunctionalInterface
public interface JpaCriteriaExecutor<PATH, RESULT, RETURN> {

    RETURN exec(CriteriaQuery<RESULT> cq, Path<PATH> root, EntityManager entityManager, Map<String, Object> hints);

    default RETURN exec(CriteriaQuery<RESULT> cq, Path<PATH> root, EntityManager entityManager) {
        return exec(cq, root, entityManager, Map.of());
    }

    static <ROOT> JpaCriteriaExecutor<ROOT, ROOT, List<ROOT>> list(Sort sort, Pagination pagination) {
        return (cq, root, em, hints) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            cq.select(root);

            if (sort.isSorted()) {
                cq.orderBy(JpaUtils.getOrders(sort, root, cb, em.getMetamodel()));
            }

            TypedQuery<ROOT> query = em.createQuery(cq);
            hints.forEach(query::setHint);

            if (pagination.isPaginated()) {
                query
                    .setFirstResult(pagination.getNumber() * pagination.getSize())
                    .setMaxResults(pagination.getSize());
            }

            return query.getResultList();
        };
    }

    static <ROOT> JpaCriteriaExecutor<ROOT, Long, Long> count() {
        return (cq, root, em, hints) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            cq.select(cb.count(root));

            TypedQuery<Long> query = em.createQuery(cq);

            hints.forEach(query::setHint);

            return query.getSingleResult();
        };
    }

    static <PATH> JpaCriteriaExecutor<PATH, Long, Boolean> exists() {
        return (cq, _, em, hints) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            cq.select(cb.literal(1L));               // SELECT 1, no count, no entidad

            TypedQuery<Long> query = em.createQuery(cq);
            hints.forEach(query::setHint);

            return !query
                .setMaxResults(1)                    // LIMIT 1 → short-circuit
                .getResultList()
                .isEmpty();
        };
    }

    static <ROOT> JpaCriteriaExecutor<ROOT, ROOT, Optional<ROOT>> first() {
        return (cq, root, entityManager, hints) -> {
            cq.select(root);

            TypedQuery<ROOT> query = entityManager.createQuery(cq);

            hints.forEach(query::setHint);

            return query
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
        };
    }
}
