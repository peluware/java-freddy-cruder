package com.peluware.freddy.cruder.springframework.jpa;

import com.peluware.freddy.cruder.jpa.JpaCriteriaExecutor;
import com.peluware.freddy.cruder.jpa.JpaQueryHelpers;
import com.peluware.freddy.cruder.jpa.SearchPredicateBuilder;
import com.peluware.freddy.cruder.springframework.SearchRepositoryEngine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.peluware.freddy.cruder.jpa.JpaCriteriaExecutor.createTypedQuery;
import static com.peluware.omnisearch.jpa.JpaUtils.findPath;

/**
 * {@link SearchRepositoryEngine} backed by JPA Criteria API.
 *
 * <p>Delegates predicate construction to {@link SearchPredicateBuilder} and applies
 * pagination and sorting via {@link org.springframework.data.support.PageableExecutionUtils}
 * to avoid unnecessary count queries on the last page.</p>
 */
public class JpaSearchRepositoryEngine implements SearchRepositoryEngine {

    private final EntityManager entityManager;
    private final SearchPredicateBuilder searchPredicateBuilder;

    public JpaSearchRepositoryEngine(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
    }

    @Override
    public <T> Page<T> findAllBySearch(Class<T> entityType, @Nullable String search, @Nullable String query, Pageable pageable) {
        var content = JpaQueryHelpers.query(
            entityManager,
            entityType,
            entityType,
            (root, cb) -> searchPredicateBuilder.build(root, cb, entityManager.getMetamodel(), search, query),
            list(pageable)
        );
        return PageableExecutionUtils.getPage(
            content,
            pageable,
            () -> countBySearch(entityType, search, query)
        );
    }

    @Override
    public <T> long countBySearch(Class<T> entityType, @Nullable String search, @Nullable String query) {
        return JpaQueryHelpers.query(
            entityManager,
            entityType,
            Long.class,
            (root, cb) -> searchPredicateBuilder.build(root, cb, entityManager.getMetamodel(), search, query),
            JpaCriteriaExecutor.count()
        );
    }

    static <SELECTED> JpaCriteriaExecutor<SELECTED, SELECTED, List<SELECTED>> list(Pageable pageable) {
        return (cq, root, em, hints) -> {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            cq.select(root);

            var sort = pageable.getSort();
            if (sort.isSorted()) {
                cq.orderBy(getOrders(sort, root, cb, em.getMetamodel()));
            }

            TypedQuery<SELECTED> query = createTypedQuery(cq, em, hints);

            if (pageable.isPaged()) {
                query
                    .setFirstResult(pageable.getPageNumber() * pageable.getPageSize())
                    .setMaxResults(pageable.getPageSize());
            }

            return query.getResultList();
        };
    }

    public static List<Order> getOrders(Sort sort, Path<?> root, CriteriaBuilder cb, Metamodel metamodel) {
        return sort.stream()
            .map(order -> {
                var path = findPath(order.getProperty(), root, metamodel, JoinType.LEFT);
                return order.getDirection() == Sort.Direction.ASC
                    ? cb.asc(path)
                    : cb.desc(path);
            })
            .toList();
    }
}
