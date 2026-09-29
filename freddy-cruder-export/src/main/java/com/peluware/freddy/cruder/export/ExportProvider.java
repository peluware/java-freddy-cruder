package com.peluware.freddy.cruder.export;

import com.peluware.domain.Sort;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Exports resources matching a search and filter criteria.
 *
 * <p>{@link #export} settles the file's name, media type and field selection upfront, without
 * reading anything — the returned {@link Export#writeTo} is where the actual read happens,
 * whenever it ends up being called.</p>
 */
public interface ExportProvider {

    /**
     * @param search optional text-based search (may be {@code null})
     * @param query  additional filtering expression, may be {@code null}
     * @param sort   sorting configuration, or {@code null} for unsorted results
     * @param fields the keys of the fields to include; empty means every field this export offers
     * @return the resolved export — its name and media type are already settled; nothing has been
     * read yet
     */
    Export export(@Nullable String search, @Nullable String query, Sort sort, List<String> fields);
}
