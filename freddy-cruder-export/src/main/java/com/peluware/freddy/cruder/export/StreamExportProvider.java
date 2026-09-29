package com.peluware.freddy.cruder.export;

import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.StreamProvider;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Exports every record a {@link StreamProvider} returns, honoring a requested field selection,
 * without holding every matching record in memory at once.
 *
 * <p>{@link Export#writeTo} opens the stream and closes it once {@link #writeRecords} returns —
 * the whole read happens inside that single call, whenever it ends up being invoked, so it takes
 * part in whatever transaction {@code streamProvider} itself guarantees around its own read (see
 * {@code StreamProvider} implementations backed by JPA, which open one if none is already active).</p>
 *
 * @param <OUTPUT> the output DTO or projection type being exported
 */
public abstract class StreamExportProvider<OUTPUT> extends AbstractExportProvider<OUTPUT> {

    /**
     * The provider that streams every matching record.
     */
    protected final StreamProvider<OUTPUT> streamProvider;

    /**
     * @param streamProvider the provider that streams every matching record
     */
    protected StreamExportProvider(StreamProvider<OUTPUT> streamProvider) {
        this.streamProvider = Objects.requireNonNull(streamProvider, "Stream provider must not be null");
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
    public Export export(@Nullable String search, @Nullable String query, Sort sort, List<String> fields) {
        var selected = ExportField.select(fields(), fields);
        return newExport(out -> {
            try (var records = streamProvider.stream(search, query, sort)) {
                writeRecords(out, selected, records);
            }
        });
    }
}
