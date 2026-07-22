package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;

/**
 * Builds the source of a Criteria query: the {@code Root}, optionally navigated to a {@code Join}.
 *
 * @param <SELECTED> the entity type the query operates on
 * @param <RESULT>   the result type of the criteria query
 */
@FunctionalInterface
public interface JpaSource<SELECTED, RESULT> {

    From<?, SELECTED> from(CriteriaQuery<RESULT> cq);

    /**
     * Source that uses the entity's {@code Root} directly, with no joins.
     *
     * @param entityClass the entity class to root the query at
     * @param <T>         the entity type
     * @param <RESULT>    the result type of the criteria query
     * @return a source that returns {@code cq.from(entityClass)}
     */
    static <T, RESULT> JpaSource<T, RESULT> root(Class<T> entityClass) {
        return cq -> cq.from(entityClass);
    }
}
