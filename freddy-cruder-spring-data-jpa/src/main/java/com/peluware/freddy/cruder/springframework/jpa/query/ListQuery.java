package com.peluware.freddy.cruder.springframework.jpa.query;

import com.peluware.freddy.cruder.jpa.JpaUtils;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.JpaQuery;
import com.peluware.freddy.cruder.jpa.query.JpaSelection;
import com.peluware.freddy.cruder.jpa.query.JpaSource;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

/**
 * Spring Data flavored list query: selects (entity or projected), filters, and applies the sort and
 * pagination of a {@link Pageable}. The Spring counterpart of the domain
 * {@code com.peluware.freddy.cruder.jpa.query.ListQuery}. For whole entities rooted at a class prefer
 * {@link EntityListQuery}. Run it with
 * {@link com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor#exec(jakarta.persistence.EntityManager, JpaQuery)}.
 *
 * @param <T> the type of the query source
 * @param <R> the row type — the entity itself or a projection
 */
public class ListQuery<T, R> implements JpaQuery<T, R, List<R>> {

    private final Class<R> resultClass;
    private final JpaSource<T, R> source;
    private final JpaSelection<T, R> selection;
    private final JpaPredicate<T> filter;
    private final Pageable pageable;

    /**
     * Selects {@code selection} from {@code source}, filtered, sorted and paginated by {@code pageable}.
     */
    public ListQuery(
        Class<R> resultClass,
        JpaSource<T, R> source,
        JpaSelection<T, R> selection,
        JpaPredicate<T> filter,
        Pageable pageable
    ) {
        this.resultClass = resultClass;
        this.source = source;
        this.selection = selection;
        this.filter = filter;
        this.pageable = pageable;
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
        this(resultClass, source, selection, filter, Pageable.unpaged());
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
    public final List<Order> orders(From<?, T> from, CriteriaBuilder cb, Metamodel metamodel) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return List.of();
        }
        return sort.stream()
            .map(order -> {
                var path = JpaUtils.findPath(order.getProperty(), from, metamodel, JoinType.LEFT);
                return order.isAscending() ? cb.asc(path) : cb.desc(path);
            })
            .toList();
    }

    @Override
    public final List<R> result(TypedQuery<R> query) {
        if (pageable.isPaged()) {
            query
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize());
        }
        return query.getResultList();
    }
}
