package com.peluware.freddy.cruder.export;

import com.peluware.domain.Sort;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Exports resources belonging to an owner and matching a search and filter criteria. Owner-scoped
 * counterpart of {@link ExportProvider}.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 */
public interface OwnedExportProvider<OWNER_ID> {

    /**
     * @param ownerId the identifier of the owning resource
     * @param search  optional text-based search (may be {@code null})
     * @param query   additional filtering expression, may be {@code null}
     * @param sort    sorting configuration, or {@code null} for unsorted results
     * @param fields  the keys of the fields to include; empty means every field this export offers
     * @return the resolved export — its name and media type are already settled; nothing has been
     * read yet
     */
    Export export(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Sort sort, List<String> fields);
}
