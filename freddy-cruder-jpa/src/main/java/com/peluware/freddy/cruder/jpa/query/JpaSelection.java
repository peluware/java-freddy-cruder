package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Selection;

/**
 * Builds the {@code SELECT} clause of a Criteria query from the query source (a {@code Root} or a
 * {@code Join}). Use {@link #self()} to select the entity, {@link #count()}/{@link #literal} for
 * common cases, or a lambda with {@code cb.construct}/{@code cb.tuple} to project onto a DTO:
 *
 * <pre>{@code
 * JpaSelection<Product, ProductSummary> selection =
 *     (from, cb) -> cb.construct(ProductSummary.class, from.get("id"), from.get("name"));
 * }</pre>
 *
 * <p>A selection that joins a plural association fans out rows; apply {@code DISTINCT} to keep a
 * paginated result consistent with its count.</p>
 *
 * @param <T> the type of the query source passed in
 * @param <R> the result type produced by the selection
 */
@FunctionalInterface
public interface JpaSelection<T, R> {

    Selection<? extends R> select(From<?, T> from, CriteriaBuilder cb);

    /**
     * Returns the identity selection, which selects the query source itself — the equivalent
     * of {@code SELECT e FROM Entity e}. Use it where a projected API must return whole
     * entities instead of repeating {@code (from, cb) -> from} at every call site.
     *
     * @param <T> the entity type
     * @return a selection that returns the source unchanged
     */
    static <T> JpaSelection<T, T> self() {
        return (from, _) -> from;
    }

    /**
     * Selects the row count of the query source — the equivalent of {@code SELECT count(e)}.
     *
     * @param <T> the entity type
     * @return a selection that produces the count
     */
    static <T> JpaSelection<T, Long> count() {
        return (from, cb) -> cb.count(from);
    }

    /**
     * Selects a constant value independent of the row — useful for existence probes
     * ({@code SELECT 1}).
     *
     * @param value the constant to select; must not be {@code null}
     * @param <T>   the entity type
     * @param <X>   the constant's type
     * @return a selection that produces {@code value} for every row
     */
    static <T, X> JpaSelection<T, X> literal(X value) {
        return (_, cb) -> cb.literal(value);
    }
}
