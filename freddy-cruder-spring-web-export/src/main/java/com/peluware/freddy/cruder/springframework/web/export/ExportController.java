package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.export.ExportProvider;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringToPeluwareAdapters;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Objects;

/**
 * Exposes an {@link ExportProvider} to download a file of every record matching a search, query,
 * sort and field selection.
 */
public interface ExportController {

    ExportProvider getExportService();

    @GetMapping("/export")
    default ResponseEntity<StreamingResponseBody> export(
        @RequestParam(name = "search", required = false) @Nullable String search,
        @RequestParam(name = "query", required = false) @Nullable String query,
        @RequestParam(name = "fields", required = false) @Nullable List<String> fields,
        Sort sort,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        var resolved = CrudContext.call(options, () -> getExportService().export(
            search,
            query,
            SpringToPeluwareAdapters.toSort(sort),
            Objects.requireNonNullElse(fields, List.of())
        ));
        return ExportResponses.response(resolved, options);
    }
}
