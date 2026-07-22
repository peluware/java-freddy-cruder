package com.peluware.freddy.cruder.jpa.query;

import com.peluware.domain.Pagination;
import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Executes a query and materializes its result — {@link #list}, {@link #single}, {@link #first},
 * {@link #exists}, {@link #stream}, and so on. Apply query hints with {@link #addHints}.
 *
 * @param <RESULT> the row type of the query
 * @param <RETURN> the value produced from the query
 */
@FunctionalInterface
public interface JpaResult<RESULT, RETURN> {

    RETURN result(TypedQuery<RESULT> query);

    default JpaResult<RESULT, RETURN> addHints(Map<String, Object> hints) {
        return query -> {
            hints.forEach(query::setHint);
            return result(query);
        };
    }

    static <RESULT> JpaResult<RESULT, List<RESULT>> list(Pagination pagination) {
        return query -> {

            if (pagination.isPaginated()) {
                query
                    .setFirstResult(pagination.getNumber() * pagination.getSize())
                    .setMaxResults(pagination.getSize());
            }

            return query.getResultList();
        };
    }

    static <RESULT> JpaResult<RESULT, List<RESULT>> list() {
        return list(Pagination.unpaginated());
    }

    static <RESULT> JpaResult<RESULT, Optional<RESULT>> first() {
        return query -> query
            .setMaxResults(1)
            .getResultList()
            .stream()
            .findFirst();
    }

    static <RESULT> JpaResult<RESULT, RESULT> firstOrThrow(Supplier<? extends RuntimeException> exceptionSupplier) {
        return query -> JpaResult.<RESULT>first().result(query).orElseThrow(exceptionSupplier);
    }

    static <RESULT> JpaResult<RESULT, RESULT> firstOr(RESULT other) {
        return query -> JpaResult.<RESULT>first().result(query).orElse(other);
    }


    static <RESULT> JpaResult<RESULT, RESULT> firstOrGet(Supplier<? extends RESULT> other) {
        return query -> JpaResult.<RESULT>first().result(query).orElseGet(other);
    }

    static <RESULT> JpaResult<RESULT, Optional<RESULT>> atMostOne() {
        return query -> {

            var results = query
                .setMaxResults(2)                    // cap at 2: enough to detect non-uniqueness
                .getResultList();

            if (results.size() > 1) {
                throw new NonUniqueResultException("Expected at most one result but found more than one");
            }

            return results.stream().findFirst();
        };
    }

    static <RESULT> JpaResult<RESULT, RESULT> single() {
        return TypedQuery::getSingleResult;
    }

    /**
     * At-most-one value, or {@link Optional#empty()} when the single row's value is {@code null}
     * or absent. Suited to scalar aggregates ({@code AVG}, {@code SUM}, …) whose value is
     * {@code null} over an empty set.
     */
    static <RESULT> JpaResult<RESULT, Optional<RESULT>> singleOptional() {
        return query -> Optional.ofNullable(query.getSingleResultOrNull());
    }

    /**
     * Whether the query matches any row, short-circuiting at the first (via {@code LIMIT 1}).
     */
    static <RESULT> JpaResult<RESULT, Boolean> exists() {
        return query -> !query
            .setMaxResults(1)
            .getResultList()
            .isEmpty();
    }

    static <RESULT> JpaResult<RESULT, Stream<RESULT>> stream() {
        return TypedQuery::getResultStream;
    }
}
