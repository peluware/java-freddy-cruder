package com.peluware.freddy.cruder.mongodb;

import org.bson.conversions.Bson;
import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for building a MongoDB {@link Bson} filter from a search string
 * and an RSQL query expression, independently of any specific search library.
 *
 * <p>The built-in default delegates to {@code omni-search-mongodb} via
 * {@link OmniSearchFilterAdapter}. Custom implementations can replace it
 * with any filter-building strategy without a dependency on {@code omni-search}.</p>
 *
 * @see OmniSearchFilterAdapter
 */
@FunctionalInterface
public interface SearchFilterBuilder {

    /**
     * Builds a {@link Bson} filter for the given document class based on the
     * provided search string and RSQL query expression.
     *
     * @param documentClass the document class being queried
     * @param search        normalized full-text search string, or {@code null}
     * @param query         RSQL filter expression, or {@code null}
     * @param <E>           the document type
     * @return a filter to apply to the collection query; must not be {@code null}
     */
    <E> Bson build(Class<E> documentClass, @Nullable String search, @Nullable String query);

}
