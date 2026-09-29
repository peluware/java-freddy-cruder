package com.peluware.freddy.cruder.export;

import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.ListProvider;
import org.jspecify.annotations.Nullable;

import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

/**
 * Exports every record a {@link ListProvider} returns, honoring a requested field selection.
 *
 * @param <OUTPUT> the output DTO or projection type being exported
 */
public abstract class ListExportProvider<OUTPUT> extends AbstractExportProvider<OUTPUT> {

    /**
     * The provider that lists every matching record.
     */
    protected final ListProvider<OUTPUT> listProvider;

    /**
     * @param listProvider the provider that lists every matching record
     */
    protected ListExportProvider(ListProvider<OUTPUT> listProvider) {
        this.listProvider = Objects.requireNonNull(listProvider, "List provider must not be null");
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
    public Export export(@Nullable String search, @Nullable String query, Sort sort, List<String> fields) {
        var selected = ExportField.select(fields(), fields);
        return newExport(out -> writeRecords(out, selected, listProvider.list(search, query, sort)));
    }
}
