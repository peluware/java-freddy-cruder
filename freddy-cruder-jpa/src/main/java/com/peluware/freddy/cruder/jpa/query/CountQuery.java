package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import org.jspecify.annotations.Nullable;

/**
 * Reusable {@code SELECT count(e)} query over a {@link JpaSource source} (root or join), optionally
 * filtered. For a plain entity rooted at a class prefer {@link EntityCountQuery}. Run it with
 * {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}.
 *
 * @param <T> the type of the query source
 */
public class CountQuery<T> implements JpaQuery<T, Long, Long> {

    private final JpaSource<T, Long> source;
    private final JpaPredicate<T> filter;

    /**
     * Counts every row of {@code source}.
     *
     * @param source the query source (root or join)
     */
    public CountQuery(JpaSource<T, Long> source) {
        this(source, JpaPredicate.all());
    }

    /**
     * Counts the rows of {@code source} matching {@code filter}.
     *
     * @param source the query source (root or join)
     * @param filter the predicate to match
     */
    public CountQuery(JpaSource<T, Long> source, JpaPredicate<T> filter) {
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
        return cb.count(from);
    }

    @Override
    public final @Nullable Predicate build(From<?, T> from, CriteriaBuilder cb) {
        return filter.build(from, cb);
    }

    @Override
    public final Long result(TypedQuery<Long> query) {
        return query.getSingleResult();
    }
}
