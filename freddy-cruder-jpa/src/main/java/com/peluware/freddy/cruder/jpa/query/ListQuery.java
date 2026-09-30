package com.peluware.freddy.cruder.jpa.query;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Reusable list query: selects (entity or projected), filters, orders, and paginates. For whole
 * entities rooted at a class prefer {@link EntityListQuery}; this class takes an explicit result
 * type, {@link JpaSource source} (root or join), and {@link JpaSelection selection} for projections.
 * Run it with {@link JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}.
 *
 * <pre>{@code
 * List<ProductDto> page = JpaQueryExecutor.exec(em, new ListQuery<>(
 *     ProductDto.class,
 *     JpaSource.root(Product.class),
 *     (from, cb) -> cb.construct(ProductDto.class, from.get("id"), from.get("name")),
 *     filter, sort, pagination));
 * }</pre>
 *
 * @param <T> the type of the query source
 * @param <R> the row type — the entity itself or a projection
 */
public class ListQuery<T, R> implements JpaQuery<T, R, List<R>> {

    final Class<R> resultClass;
    final JpaSource<T, R> source;
    final JpaSelection<T, R> selection;
    final JpaPredicate<T> filter;
    final JpaGroupBy<T> groupBy;
    final Sort sort;
    final Pagination pagination;
    final boolean distinct;

    /**
     * Selects {@code selection} from {@code source}, filtered, grouped, sorted and paginated.
     *
     * @param distinct whether to deduplicate rows ({@code SELECT DISTINCT}) — needed when
     *                 {@code filter} or {@code source} join a to-many relation, which can otherwise
     *                 repeat a row once per match on the joined side
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        JpaGroupBy<T> groupBy,
        Sort sort,
        Pagination pagination,
        boolean distinct
    ) {
        this.resultClass = resultClass;
        this.source = source;
        this.selection = selection;
        this.filter = filter;
        this.groupBy = groupBy;
        this.sort = sort;
        this.pagination = pagination;
        this.distinct = distinct;
    }

    /**
     * Selects {@code selection} from {@code source}, filtered, grouped, sorted and paginated — no
     * row deduplication.
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        JpaGroupBy<T> groupBy,
        Sort sort,
        Pagination pagination
    ) {
        this(resultClass, source, selection, filter, groupBy, sort, pagination, false);
    }

    /**
     * Selects {@code selection} from {@code source}, filtered, sorted and paginated — no grouping.
     *
     * @param distinct whether to deduplicate rows ({@code SELECT DISTINCT})
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        Sort sort,
        Pagination pagination,
        boolean distinct
    ) {
        this(resultClass, source, selection, filter, JpaGroupBy.none(), sort, pagination, distinct);
    }

    /**
     * Selects {@code selection} from {@code source}, filtered, sorted and paginated — no grouping,
     * no row deduplication.
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        Sort sort,
        Pagination pagination
    ) {
        this(resultClass, source, selection, filter, JpaGroupBy.none(), sort, pagination, false);
    }

    /**
     * Sorted, without pagination — returns every matching row.
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        Sort sort
    ) {
        this(resultClass, source, selection, filter, sort, Pagination.unpaginated());
    }

    /**
     * Paginated, without ordering.
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        Pagination pagination
    ) {
        this(resultClass, source, selection, filter, Sort.unsorted(), pagination);
    }

    /**
     * Neither sorted nor paginated — every matching row, in store order.
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter
    ) {
        this(resultClass, source, selection, filter, Sort.unsorted(), Pagination.unpaginated());
    }

    /**
     * A copy of this query with row deduplication ({@code SELECT DISTINCT}) turned on — needed when
     * {@code filter} or {@code source} join a to-many relation, which can otherwise repeat a row once
     * per match on the joined side. Combine with any constructor, regardless of what it sets sort or
     * pagination to.
     */
    public ListQuery<T, R> distinct() {
        return this.distinct(true);
    }

    /**
     * A copy of this query with row deduplication ({@code SELECT DISTINCT}) set to {@code distinct}.
     */
    public ListQuery<T, R> distinct(boolean distinct) {
        return new ListQuery<>(resultClass, source, selection, filter, groupBy, sort, pagination, distinct);
    }

    @Override
    public final CriteriaQuery<R> create(CriteriaBuilder cb) {
        var cq = cb.createQuery(resultClass);
        cq.distinct(distinct);
        return cq;
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
    public final List<Order> orders(From<?, T> from, CriteriaBuilder cb, Metamodel metamodel) {
        return JpaOrder.<T>by(sort).orders(from, cb, metamodel);
    }

    @Override
    public final List<R> result(TypedQuery<R> query) {
        return JpaResult.<R>list(pagination).result(query);
    }
}
