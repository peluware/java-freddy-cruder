package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Reusable find query: returns the first row matching the filter, or throws the supplied exception
 * when there is none. For a plain entity rooted at a class prefer {@link EntityFindQuery}. Run it
 * with {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}.
 *
 * @param <T> the type of the query source
 * @param <R> the row type — the entity itself or a projection
 */
public class FindQuery<T, R> implements JpaQuery<T, R, R> {

    final Class<R> resultClass;
    final JpaSource<T, R> source;
    final JpaSelection<T, R> selection;
    final JpaPredicate<T> filter;
    final JpaGroupBy<T> groupBy;
    final Supplier<? extends RuntimeException> onEmpty;

    public FindQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        JpaGroupBy<T> groupBy,
        Supplier<? extends RuntimeException> onEmpty
    ) {
        this.resultClass = resultClass;
        this.source = source;
        this.selection = selection;
        this.filter = filter;
        this.groupBy = groupBy;
        this.onEmpty = onEmpty;
    }

    /**
     * Same as above, without grouping.
     */
    public FindQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        Supplier<? extends RuntimeException> onEmpty
    ) {
        this(resultClass, source, selection, filter, JpaGroupBy.none(), onEmpty);
    }

    @Override
    public final CriteriaQuery<R> create(CriteriaBuilder cb) {
        return cb.createQuery(resultClass);
    }

    @Override
    public final From<?, T> from(CriteriaQuery<R> cq) {
        return source.from(cq);
    }

    @Override
    public final Selection<? extends R> select(From<?, T> from, CriteriaBuilder cb) {
        return selection.select(from, cb);
    }

    @Override
    public final @Nullable Predicate build(From<?, T> from, CriteriaBuilder cb) {
        return filter.build(from, cb);
    }

    @Override
    public final List<Expression<?>> groupBy(From<?, T> from, CriteriaBuilder cb) {
        return groupBy.groupBy(from, cb);
    }

    @Override
    public final R result(TypedQuery<R> query) {
        return JpaResult.<R>firstOrThrow(onEmpty).result(query);
    }
}
