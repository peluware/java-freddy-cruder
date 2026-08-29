package com.peluware.freddy.cruder.jpa;

import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
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
     * Builds a {@link Predicate} for the given query from based on the provided
     * search string and RSQL query expression.
     *
     * @param from     the query from of the entity being queried
     * @param cb       the criteria builder
     * @param metamodel the JPA metamodel, used to resolve entity attributes
     * @param search   normalized full-text search string, or {@code null}
     * @param query    RSQL filter expression, or {@code null}
     * @param <E>      the entity type of the query from
     * @return a predicate to apply to the query, or {@code null} to leave it unrestricted
     */
    <E> @Nullable Predicate build(From<?, E> from, CriteriaBuilder cb, Metamodel metamodel, @Nullable String search, @Nullable String query);

    /**
     * Binds the metamodel and the search/query strings into a {@link JpaPredicate}, ready to use
     * as the {@code WHERE} of a query.
     *
     * <pre>{@code
     * JpaQueryExecutor.exec(em, Product.class,
     *     searchPredicateBuilder.bind(em.getMetamodel(), search, query),
     *     JpaResult.list(pagination));
     * }</pre>
     *
     * @param metamodel the JPA metamodel, used to resolve entity attributes
     * @param search    normalized full-text search string, or {@code null}
     * @param query     RSQL filter expression, or {@code null}
     * @param <E>       the entity type of the query root
     * @return a predicate bound to the given search parameters
     */
    default <E> JpaPredicate<E> bind(Metamodel metamodel, @Nullable String search, @Nullable String query) {
        return (root, cb) -> build(root, cb, metamodel, search, query);
    }
}
