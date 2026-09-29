package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.export.OwnedExportProvider;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringToPeluwareAdapters;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Objects;

/**
 * Exposes an {@link OwnedExportProvider} to download a file of every record of an owner matching a
 * search, query, sort and field selection.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 */
public interface OwnedExportController<OWNER_ID> {

    OwnedExportProvider<OWNER_ID> getExportService();

    @GetMapping("/export")
    default ResponseEntity<StreamingResponseBody> export(
        @PathVariable("ownerId") OWNER_ID ownerId,
        @RequestParam(name = "search", required = false) @Nullable String search,
        @RequestParam(name = "query", required = false) @Nullable String query,
        @RequestParam(name = "fields", required = false) @Nullable List<String> fields,
        Sort sort,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        var resolved = CrudContext.call(options, () -> getExportService().export(
            ownerId,
            search,
            query,
            SpringToPeluwareAdapters.toSort(sort),
            Objects.requireNonNullElse(fields, List.of())
        ));
        return ExportResponses.response(resolved, options);
    }
}
