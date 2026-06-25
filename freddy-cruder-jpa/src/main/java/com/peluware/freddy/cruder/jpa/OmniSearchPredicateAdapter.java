package com.peluware.freddy.cruder.jpa;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.jpa.DefaultJpaOmniSearchPredicateBuilder;
import com.peluware.omnisearch.jpa.JpaOmniSearchPredicateBuilder;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;

/**
 * Adapter that bridges {@link JpaOmniSearchPredicateBuilder} (from {@code omni-search-jpa})
 * to the {@link SearchPredicateBuilder} interface defined in {@code freddy-cruder-jpa}.
 *
 * <p>
 * Use this adapter to plug any existing {@link JpaOmniSearchPredicateBuilder} implementation
 * into providers that expect a {@link SearchPredicateBuilder}:
 * </p>
 *
 * <pre>{@code
 * SearchPredicateBuilder builder = new OmniSearchPredicateAdapter(myCustomBuilder);
 * }</pre>
 *
 * <p>
 * A pre-built default wrapping {@link DefaultJpaOmniSearchPredicateBuilder} is available
 * via {@link #ofDefault()}.
 * </p>
 *
 * @see SearchPredicateBuilder
 * @see JpaOmniSearchPredicateBuilder
 */
public final class OmniSearchPredicateAdapter implements SearchPredicateBuilder {

    private final JpaOmniSearchPredicateBuilder delegate;

    /**
     * Creates an adapter wrapping the given {@link JpaOmniSearchPredicateBuilder}.
     *
     * @param delegate the predicate builder to delegate to
     */
    public OmniSearchPredicateAdapter(JpaOmniSearchPredicateBuilder delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns an adapter wrapping a {@link DefaultJpaOmniSearchPredicateBuilder},
     * which is the standard out-of-the-box predicate builder from {@code omni-search-jpa}.
     *
     * @return a default {@link SearchPredicateBuilder} backed by {@code omni-search-jpa}
     */
    public static OmniSearchPredicateAdapter ofDefault() {
        return new OmniSearchPredicateAdapter(new DefaultJpaOmniSearchPredicateBuilder());
    }

    @Override
    public <E> Predicate build(From<?, E> root, CriteriaBuilder cb, Metamodel metamodel,
                               @Nullable String search, @Nullable String query) {
        return delegate.buildPredicate(
            root,
            new OmniSearchBaseOptions().search(search).query(query),
            cb,
            metamodel
        );
    }
}
