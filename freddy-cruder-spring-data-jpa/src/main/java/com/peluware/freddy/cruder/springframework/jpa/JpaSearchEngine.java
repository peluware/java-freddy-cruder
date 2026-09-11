package com.peluware.freddy.cruder.springframework.jpa;

import com.peluware.freddy.cruder.jpa.SearchPredicateBuilder;
import com.peluware.freddy.cruder.jpa.query.EntityCountQuery;
import com.peluware.freddy.cruder.jpa.query.JpaPredicate;
import com.peluware.freddy.cruder.jpa.query.JpaQueryExecutor;
import com.peluware.freddy.cruder.jpa.query.JpaSelection;
import com.peluware.freddy.cruder.jpa.query.JpaSource;
import com.peluware.freddy.cruder.springframework.SearchEngine;
import com.peluware.freddy.cruder.springframework.jpa.query.EntityListQuery;
import com.peluware.freddy.cruder.springframework.jpa.query.ListQuery;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

/**
 * {@link SearchEngine} backed by JPA Criteria API.
 *
 * <p>Delegates predicate construction to {@link SearchPredicateBuilder} and applies
 * pagination and sorting via {@link org.springframework.data.support.PageableExecutionUtils}
 * to avoid unnecessary count queries on the last page.</p>
 */
public class JpaSearchEngine implements SearchEngine {

    private final EntityManager entityManager;
    private final SearchPredicateBuilder searchPredicateBuilder;

    public JpaSearchEngine(EntityManager entityManager, SearchPredicateBuilder searchPredicateBuilder) {
        this.entityManager = entityManager;
        this.searchPredicateBuilder = searchPredicateBuilder;
    }

    @Override
    public <T> Page<T> findAllBySearch(Class<T> entityType, @Nullable String search, @Nullable String query, Pageable pageable) {
        var predicate = this.<T>searchPredicate(search, query);
        var content = JpaQueryExecutor.exec(
            entityManager,
            new EntityListQuery<>(
                entityType,
                predicate,
                pageable
            )
        );
        return PageableExecutionUtils.getPage(
            content,
            pageable,
            () -> count(entityType, predicate)
        );
    }

    @Override
    public <T> List<T> findAllBySearch(Class<T> entityType, @Nullable String search, @Nullable String query, Sort sort) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityListQuery<>(
                entityType,
                this.searchPredicate(search, query),
                Pageable.unpaged(sort)
            )
        );
    }

    /**
     * Runs a paginated search combining the search/query predicate with an additional
     * consumer-supplied {@code filter} via a logical {@code AND}, returning full entities.
     *
     * <p>Use this to scope a search from the caller — e.g. an active flag, an owner, or a
     * tenant — without pushing the condition into the {@code query} string.</p>
     *
     * @param entityType the entity class used as the query root
     * @param search     normalized full-text search string, or {@code null}
     * @param query      RSQL query expression, or {@code null}
     * @param pageable   the pagination and sort specification
     * @param filter     an additional predicate combined with the search via {@code AND}
     * @param <T>        the entity type
     * @return a page of matching entities
     */
    public <T> Page<T> findAllBySearch(
        Class<T> entityType,
        @Nullable String search,
        @Nullable String query,
        Pageable pageable,
        JpaPredicate<T> filter
    ) {
        var predicate = this.<T>searchPredicate(search, query).and(filter);
        var content = JpaQueryExecutor.exec(
            entityManager,
            new EntityListQuery<>(
                entityType,
                predicate,
                pageable
            )
        );
        return PageableExecutionUtils.getPage(
            content,
            pageable,
            () -> count(entityType, predicate)
        );
    }

    /**
     * Runs a paginated search that projects each matching entity to {@code resultType}
     * at the database level, instead of loading the full entity.
     *
     * <p>The predicate is built from {@code search} and {@code query} exactly as in the
     * non-projected overload; only the {@code SELECT} clause differs. The total count is
     * resolved over entity rows with the same predicate, so {@code selection} must select
     * scalar attributes — a selection that joins a plural association fans out rows and
     * desynchronizes the page content from its count.</p>
     *
     * @param entityType the entity class used as the query root
     * @param resultType the projected result type produced by {@code selection}
     * @param search     normalized full-text search string, or {@code null}
     * @param query      RSQL query expression, or {@code null}
     * @param pageable   the pagination and sort specification
     * @param selection  builds the {@code SELECT} clause from the root and criteria builder
     * @param <T>        the entity type
     * @param <R>        the projected result type
     * @return a page of projected results
     */
    public <T, R> Page<R> findAllBySearch(
        Class<T> entityType,
        Class<R> resultType,
        @Nullable String search,
        @Nullable String query,
        Pageable pageable,
        JpaSelection<T, R> selection
    ) {
        var predicate = this.<T>searchPredicate(search, query);
        var content = JpaQueryExecutor.exec(
            entityManager,
            new ListQuery<>(
                resultType,
                JpaSource.root(entityType),
                selection,
                predicate,
                pageable
            )
        );
        return PageableExecutionUtils.getPage(
            content,
            pageable,
            () -> count(entityType, predicate)
        );
    }

    /**
     * Runs a paginated projected search combining the search/query predicate with an
     * additional consumer-supplied {@code filter} via a logical {@code AND}.
     *
     * <p>Combines the selection of {@link #findAllBySearch(Class, Class, String, String, Pageable, JpaSelection)}
     * with the caller-supplied scoping of {@link #findAllBySearch(Class, String, String, Pageable, JpaPredicate)}.
     * The same scalar-selection caveat applies.</p>
     *
     * @param entityType the entity class used as the query root
     * @param resultType the projected result type produced by {@code selection}
     * @param search     normalized full-text search string, or {@code null}
     * @param query      RSQL query expression, or {@code null}
     * @param pageable   the pagination and sort specification
     * @param filter     an additional predicate combined with the search via {@code AND}
     * @param selection  builds the {@code SELECT} clause from the root and criteria builder
     * @param <T>        the entity type
     * @param <R>        the projected result type
     * @return a page of projected results
     */
    public <T, R> Page<R> findAllBySearch(
        Class<T> entityType,
        Class<R> resultType,
        @Nullable String search,
        @Nullable String query,
        Pageable pageable,
        JpaPredicate<T> filter,
        JpaSelection<T, R> selection
    ) {
        var predicate = this.<T>searchPredicate(search, query).and(filter);
        var content = JpaQueryExecutor.exec(
            entityManager,
            new ListQuery<>(
                resultType,
                JpaSource.root(entityType),
                selection,
                predicate,
                pageable
            )
        );
        return PageableExecutionUtils.getPage(
            content,
            pageable,
            () -> count(entityType, predicate)
        );
    }

    @Override
    public <T> long countBySearch(
        Class<T> entityType,
        @Nullable String search,
        @Nullable String query
    ) {
        return count(entityType, this.searchPredicate(search, query));
    }

    protected <T> JpaPredicate<T> searchPredicate(
        @Nullable String search,
        @Nullable String query
    ) {
        return searchPredicateBuilder.bind(entityManager.getMetamodel(), search, query);
    }

    protected <T> long count(Class<T> entityType, JpaPredicate<T> predicate) {
        return JpaQueryExecutor.exec(
            entityManager,
            new EntityCountQuery<>(entityType, predicate)
        );
    }
}
