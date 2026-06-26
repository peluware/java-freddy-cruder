package com.peluware.freddy.cruder.jpa;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.jpa.DefaultJpaOmniSearchPredicateBuilder;
import com.peluware.omnisearch.jpa.JpaOmniSearchPredicateBuilder;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;

import java.util.Set;

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
 * To include join propagations in every search query, pass them at construction time:
 * </p>
 *
 * <pre>{@code
 * SearchPredicateBuilder builder = new OmniSearchPredicateAdapter(myCustomBuilder, Set.of("tags", "category"));
 * }</pre>
 *
 * <p>
 * A pre-built default wrapping {@link DefaultJpaOmniSearchPredicateBuilder} is available
 * via {@link #ofDefault()} and {@link #ofDefault(Set)}.
 * </p>
 *
 * @see SearchPredicateBuilder
 * @see JpaOmniSearchPredicateBuilder
 */
public final class OmniSearchPredicateAdapter implements SearchPredicateBuilder {

    private final JpaOmniSearchPredicateBuilder delegate;
    private final Set<String> propagations;

    /**
     * Creates an adapter wrapping the given {@link JpaOmniSearchPredicateBuilder}
     * with the specified join propagations applied to every search query.
     *
     * @param delegate     the predicate builder to delegate to
     * @param propagations join associations to include in every search query
     */
    public OmniSearchPredicateAdapter(JpaOmniSearchPredicateBuilder delegate, Set<String> propagations) {
        this.delegate = delegate;
        this.propagations = propagations;
    }

    /**
     * Creates an adapter wrapping the given {@link JpaOmniSearchPredicateBuilder}
     * with no join propagations.
     *
     * @param delegate the predicate builder to delegate to
     */
    public OmniSearchPredicateAdapter(JpaOmniSearchPredicateBuilder delegate) {
        this(delegate, Set.of());
    }

    /**
     * Returns an adapter wrapping a {@link DefaultJpaOmniSearchPredicateBuilder}
     * with the specified join propagations applied to every search query.
     *
     * @param propagations join associations to include in every search query
     * @return a {@link SearchPredicateBuilder} backed by {@code omni-search-jpa}
     */
    public static OmniSearchPredicateAdapter ofDefault(Set<String> propagations) {
        return new OmniSearchPredicateAdapter(new DefaultJpaOmniSearchPredicateBuilder(), propagations);
    }

    /**
     * Returns an adapter wrapping a {@link DefaultJpaOmniSearchPredicateBuilder}
     * with no join propagations.
     *
     * @return a default {@link SearchPredicateBuilder} backed by {@code omni-search-jpa}
     */
    public static OmniSearchPredicateAdapter ofDefault() {
        return new OmniSearchPredicateAdapter(new DefaultJpaOmniSearchPredicateBuilder());
    }

    @Override
    public <E> Predicate build(From<?, E> root, CriteriaBuilder cb, Metamodel metamodel, @Nullable String search, @Nullable String query) {
        return delegate.buildPredicate(
            root,
            new OmniSearchBaseOptions().search(search).query(query).propagations(propagations),
            cb,
            metamodel
        );
    }
}
