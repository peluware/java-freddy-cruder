package com.peluware.freddy.cruder.jpa.query;

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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link JpaQuery} that delegates every part of the query to another one and sets JPA query hints
 * on the {@link TypedQuery} before the delegate materializes its result. The hints can be
 * built with {@link JpaHints}.
 *
 * <pre>{@code
 * var query = new JpaQueryHints<>(new EntityListQuery<>(Product.class, predicate, sort),
 *                                 JpaHints.fetchGraph(graph));
 * }</pre>
 *
 * @param <SELECTED> the entity type of the query source
 * @param <RESULT>   the row type produced by the selection
 * @param <RETURN>   the value produced by the result
 * @see JpaHints
 */
public final class JpaQueryHints<SELECTED, RESULT, RETURN> implements JpaQuery<SELECTED, RESULT, RETURN> {

    private final JpaQuery<SELECTED, RESULT, RETURN> delegate;
    private final Map<String, Object> hints;

    /**
     * @param delegate the query to run
     * @param hints    the JPA query hints to set on it
     */
    public JpaQueryHints(JpaQuery<SELECTED, RESULT, RETURN> delegate, Map<String, Object> hints) {
        this.delegate = delegate;
        this.hints = Collections.unmodifiableMap(new LinkedHashMap<>(hints));
    }

    @Override
    public CriteriaQuery<RESULT> create(CriteriaBuilder cb) {
        return delegate.create(cb);
    }

    @Override
    public From<?, SELECTED> from(CriteriaQuery<RESULT> cq) {
        return delegate.from(cq);
    }

    @Override
    public Selection<? extends RESULT> select(From<?, SELECTED> from, CriteriaBuilder cb) {
        return delegate.select(from, cb);
    }

    @Override
    public @Nullable Predicate build(From<?, SELECTED> from, CriteriaBuilder cb) {
        return delegate.build(from, cb);
    }

    @Override
    public List<Expression<?>> groupBy(From<?, SELECTED> from, CriteriaBuilder cb) {
        return delegate.groupBy(from, cb);
    }

    @Override
    public List<Order> orders(From<?, SELECTED> from, CriteriaBuilder cb, Metamodel metamodel) {
        return delegate.orders(from, cb, metamodel);
    }

    @Override
    public RETURN result(TypedQuery<RESULT> query) {
        hints.forEach(query::setHint);
        return delegate.result(query);
    }
}
