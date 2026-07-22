package com.peluware.freddy.cruder.mongodb;

import com.peluware.omnisearch.OmniSearchBaseOptions;
import com.peluware.omnisearch.mongodb.MongoOmniSearchFilterBuilder;
import org.bson.conversions.Bson;
import org.jspecify.annotations.Nullable;

import java.util.Set;

/**
 * Adapter that bridges {@link MongoOmniSearchFilterBuilder} (from {@code omni-search-mongodb})
 * to the {@link SearchFilterBuilder} interface defined in {@code freddy-cruder-mongodb}.
 *
 * <p>To include join propagations in every query, pass them at construction time:</p>
 *
 * <pre>{@code
 * SearchFilterBuilder builder = new OmniSearchFilterAdapter(myBuilder, Set.of("tags"));
 * }</pre>
 *
 * @see SearchFilterBuilder
 * @see MongoOmniSearchFilterBuilder
 */
public class OmniSearchFilterAdapter implements SearchFilterBuilder {

    private final MongoOmniSearchFilterBuilder delegate;
    private final Set<String> propagations;

    public OmniSearchFilterAdapter(MongoOmniSearchFilterBuilder delegate, Set<String> propagations) {
        this.delegate = delegate;
        this.propagations = propagations;
    }

    public OmniSearchFilterAdapter(MongoOmniSearchFilterBuilder delegate) {
        this(delegate, Set.of());
    }

    @Override
    public <E> Bson build(Class<E> documentClass, @Nullable String search, @Nullable String query) {
        return delegate.buildFilter(
            documentClass,
            new OmniSearchBaseOptions()
                .search(search)
                .query(query)
                .propagations(propagations)
        );
    }
}
