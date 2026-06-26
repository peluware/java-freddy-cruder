package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for building a JPA {@link Predicate} from a search string
 * and an RSQL query expression, independently of any specific search library.
 *
 * <p>
 * Implementations receive the query root, the criteria builder, the JPA metamodel,
 * and the raw search/query strings, and are responsible for returning a predicate
 * that can be applied directly to a {@link jakarta.persistence.criteria.CriteriaQuery}.
 * </p>
 *
 * <p>
 * The built-in default delegates to {@code omni-search-jpa} via
 * {@link OmniSearchPredicateAdapter}. Custom implementations can replace it
 * with any predicate-building strategy without a dependency on {@code omni-search}.
 * </p>
 *
 * @see OmniSearchPredicateAdapter
 */
@FunctionalInterface
public interface SearchPredicateBuilder {

    /**
     * Builds a {@link Predicate} for the given query root based on the provided
     * search string and RSQL query expression.
     *
     * @param root     the query root of the entity being queried
     * @param cb       the criteria builder
     * @param metamodel the JPA metamodel, used to resolve entity attributes
     * @param search   normalized full-text search string, or {@code null}
     * @param query    RSQL filter expression, or {@code null}
     * @param <E>      the entity type of the query root
     * @return a predicate to apply to the query; must not be {@code null}
     */
    <E> Predicate build(From<?, E> root, CriteriaBuilder cb, Metamodel metamodel, @Nullable String search, @Nullable String query);
}
