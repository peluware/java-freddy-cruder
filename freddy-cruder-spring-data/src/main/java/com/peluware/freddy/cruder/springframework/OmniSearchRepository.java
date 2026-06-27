package com.peluware.freddy.cruder.springframework;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.omnisearch.EntityOmniSearch;
import com.peluware.omnisearch.OmniSearch;
import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.OmniSearchOptions;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;

/**
 * Adapter that bridges {@link EntityOmniSearch} (from {@code omni-search}) to the
 * {@link SearchRepository} interface defined in {@code freddy-cruder-spring-data}.
 *
 * <p>
 * Use this adapter to plug any existing {@link EntityOmniSearch} implementation into
 * providers that expect an {@link SearchRepository}:
 * </p>
 *
 * <pre>{@code
 * SearchRepository<Product> search = new OmniSearchRepository<>(myOmniSearch);
 * }</pre>
 *
 * <p>
 * To include join propagations in every search query, pass them at construction time:
 * </p>
 *
 * <pre>{@code
 * SearchRepository<Product> search = new OmniSearchRepository<>(myOmniSearch, Set.of("tags", "category"));
 * }</pre>
 *
 * @param <ENTITY> the entity type
 * @see SearchRepository
 * @see EntityOmniSearch
 */
public final class OmniSearchRepository<ENTITY> implements SearchRepository<ENTITY> {

    private final EntityOmniSearch<ENTITY> delegate;
    private final Set<String> propagations;

    /**
     * Creates an adapter wrapping the given {@link EntityOmniSearch}
     * with the specified join propagations applied to every search query.
     *
     * @param delegate     the omni-search implementation to delegate to
     * @param propagations join associations to include in every search query
     */
    public OmniSearchRepository(EntityOmniSearch<ENTITY> delegate, Set<String> propagations) {
        this.delegate = delegate;
        this.propagations = propagations;
    }

    /**
     * Creates an adapter wrapping the given {@link EntityOmniSearch}
     * with no join propagations.
     *
     * @param delegate the omni-search implementation to delegate to
     */
    public OmniSearchRepository(EntityOmniSearch<ENTITY> delegate) {
        this(delegate, Set.of());
    }

    /**
     * Creates an adapter from a generic {@link OmniSearch} instance by scoping it to
     * the given entity class via {@link OmniSearch#forEntity(Class)}, with the specified
     * join propagations applied to every search query.
     *
     * @param omniSearch   the shared omni-search instance
     * @param entityClass  the entity class to scope searches to
     * @param propagations join associations to include in every search query
     */
    public OmniSearchRepository(OmniSearch omniSearch, Class<ENTITY> entityClass, Set<String> propagations) {
        this(omniSearch.forEntity(entityClass), propagations);
    }

    /**
     * Creates an adapter from a generic {@link OmniSearch} instance by scoping it to
     * the given entity class via {@link OmniSearch#forEntity(Class)}, with no join propagations.
     *
     * <p>
     * Useful when you have a shared {@link OmniSearch} bean and need to create
     * a per-entity adapter without calling {@code forEntity} yourself:
     * </p>
     *
     * <pre>{@code
     * SearchRepository<Product> search = new OmniSearchRepository<>(omniSearch, Product.class);
     * }</pre>
     *
     * @param omniSearch  the shared omni-search instance
     * @param entityClass the entity class to scope searches to
     */
    public OmniSearchRepository(OmniSearch omniSearch, Class<ENTITY> entityClass) {
        this(omniSearch.forEntity(entityClass));
    }

    @Override
    public Page<ENTITY> findAllBySearch(@Nullable String search, @Nullable String query, Pageable pageable) {
        return PeluwareToSpringAdapters.applyAsPage(pageable, (pagination, sort) -> delegate.page(new OmniSearchOptions()
            .search(search)
            .query(query)
            .propagations(propagations)
            .pagination(pagination)
            .sort(sort)
        ));
    }

    @Override
    public long countBySearch(@Nullable String search, @Nullable String query) {
        return delegate.count(new OmniSearchBaseOptions()
            .search(search)
            .query(query)
            .propagations(propagations)
        );
    }
}
