package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Functional interface for configuring and executing a JPA Criteria API query
 * given a pre-built {@link CriteriaBuilder}, {@link CriteriaQuery}, and {@link Root}.
 *
 * <p>Used with {@link JpaQueryHelpers#query} to eliminate the repeated setup boilerplate.</p>
 *
 * @param <ENTITY> the entity type of the query root
 * @param <RESULT> the result row type of the query
 * @param <RETURN>      the final return type
 * @see JpaQueryHelpers#query(jakarta.persistence.EntityManager, Class, Class, JpaCriteriaCallback)
 */
@FunctionalInterface
public interface JpaCriteriaCallback<ENTITY, RESULT, RETURN> {

    RETURN apply(CriteriaBuilder cb, CriteriaQuery<RESULT> cq, Root<ENTITY> root);
}
