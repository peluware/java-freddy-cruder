package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import org.jspecify.annotations.Nullable;

/**
 * Reusable existence probe over a {@link JpaSource source} (root or join), optionally filtered.
 * Selects a constant and stops at the first row ({@code SELECT 1 ... LIMIT 1}) rather than counting
 * — cheaper than {@code count(*) > 0}. For a plain entity rooted at a class prefer
 * {@link EntityExistsQuery}. Run it with
 * {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}.
 *
 * @param <T> the type of the query source
 */
public class ExistsQuery<T> implements JpaQuery<T, Long, Boolean> {

    final JpaSource<T, Long> source;
    final JpaPredicate<T> filter;

    /**
     * Whether {@code source} has any row.
     *
     * @param source the query source (root or join)
     */
    public ExistsQuery(JpaSource<T, Long> source) {
        this(source, JpaPredicate.all());
    }

    /**
     * Whether {@code source} has any row matching {@code filter}.
     *
     * @param source the query source (root or join)
     * @param filter the predicate to match
     */
    public ExistsQuery(JpaSource<T, Long> source, JpaPredicate<T> filter) {
        this.source = source;
        this.filter = filter;
    }

    @Override
    public final CriteriaQuery<Long> create(CriteriaBuilder cb) {
        return cb.createQuery(Long.class);
    }

    @Override
    public final From<?, T> from(CriteriaQuery<Long> cq) {
        return source.from(cq);
    }

    @Override
    public final Selection<? extends Long> select(From<?, T> from, CriteriaBuilder cb) {
        return cb.literal(1L);
    }

    @Override
    public final @Nullable Predicate build(From<?, T> from, CriteriaBuilder cb) {
        return filter.build(from, cb);
    }

    @Override
    public final Boolean result(TypedQuery<Long> query) {
        return !query
            .setMaxResults(1)
            .getResultList()
            .isEmpty();
    }
}
