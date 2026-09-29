package com.peluware.freddy.cruder.export;

import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.OwnedStreamProvider;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Exports every record an {@link OwnedStreamProvider} returns for a given owner, honoring a
 * requested field selection, without holding every matching record in memory at once.
 * Owner-scoped counterpart of {@link StreamExportProvider}.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type being exported
 */
public abstract class OwnedStreamExportProvider<OWNER_ID, OUTPUT> extends AbstractOwnedExportProvider<OWNER_ID, OUTPUT> {

    /**
     * The provider that streams every matching record of an owner.
     */
    protected final OwnedStreamProvider<OWNER_ID, OUTPUT> streamProvider;

    /**
     * @param streamProvider the provider that streams every matching record of an owner
     */
    protected OwnedStreamExportProvider(OwnedStreamProvider<OWNER_ID, OUTPUT> streamProvider) {
        this.streamProvider = Objects.requireNonNull(streamProvider, "Owned stream provider must not be null");
    }

    /**
     * Writes the selected fields of every record to {@code out}, reading {@code records} as far as
     * needed — it is closed once this returns, not before.
     *
     * @param out     where to write; not closed
     * @param fields  the fields to include, in the order they should appear
     * @param records every matching record, lazily; do not let it escape this call
     */
    protected abstract void writeRecords(OutputStream out, List<ExportField<OUTPUT>> fields, Stream<OUTPUT> records);

    @Override
    public Export export(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Sort sort, List<String> fields) {
        var selected = ExportField.select(fields(), fields);
        return newExport(out -> {
            try (var records = streamProvider.stream(ownerId, search, query, sort)) {
                writeRecords(out, selected, records);
            }
        });
    }
}
