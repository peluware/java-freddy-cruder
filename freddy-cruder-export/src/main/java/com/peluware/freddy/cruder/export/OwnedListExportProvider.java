package com.peluware.freddy.cruder.export;

import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.OwnedListProvider;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

/**
 * Exports every record an {@link OwnedListProvider} returns for a given owner, honoring a
 * requested field selection. Owner-scoped counterpart of {@link ListExportProvider}.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type being exported
 */
public abstract class OwnedListExportProvider<OWNER_ID, OUTPUT> extends AbstractOwnedExportProvider<OWNER_ID, OUTPUT> {

    /**
     * The provider that lists every matching record of an owner.
     */
    protected final OwnedListProvider<OWNER_ID, OUTPUT> listProvider;

    /**
     * @param listProvider the provider that lists every matching record of an owner
     */
    protected OwnedListExportProvider(OwnedListProvider<OWNER_ID, OUTPUT> listProvider) {
        this.listProvider = Objects.requireNonNull(listProvider, "Owned list provider must not be null");
    }

    /**
     * Writes the selected fields of every record to {@code out}.
     *
     * @param out     where to write; not closed
     * @param fields  the fields to include, in the order they should appear
     * @param records every matching record
     */
    protected abstract void writeRecords(OutputStream out, List<ExportField<OUTPUT>> fields, List<OUTPUT> records);

    @Override
    public Export export(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Sort sort, List<String> fields) {
        var selected = ExportField.select(fields(), fields);
        return newExport(out -> writeRecords(out, selected, listProvider.list(ownerId, search, query, sort)));
    }
}
