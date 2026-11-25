package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.TypedQuery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A {@link JpaResult} that sets JPA query hints on the {@link TypedQuery} before the result it wraps
 * materializes it.
 *
 * <pre>{@code
 * var result = new JpaResultHints<>(JpaResult.<Product>list(), JpaHints.fetchGraph(graph));
 * }</pre>
 *
 * @param <RESULT> the row type of the query
 * @param <RETURN> the value produced from the query
 * @see JpaQueryHints
 * @see JpaHints
 */
public final class JpaResultHints<RESULT, RETURN> implements JpaResult<RESULT, RETURN> {

    private final JpaResult<RESULT, RETURN> delegate;
    private final Map<String, Object> hints;

    /**
     * @param delegate the result to materialize
     * @param hints    the JPA query hints to set on the query first
     */
    public JpaResultHints(JpaResult<RESULT, RETURN> delegate, Map<String, Object> hints) {
        this.delegate = delegate;
        this.hints = Collections.unmodifiableMap(new LinkedHashMap<>(hints));
    }

    @Override
    public RETURN result(TypedQuery<RESULT> query) {
        hints.forEach(query::setHint);
        return delegate.result(query);
    }
}
