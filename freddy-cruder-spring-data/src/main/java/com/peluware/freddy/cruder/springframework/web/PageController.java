package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.PageProvider;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.PeluwareToSpringAdapters;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


public interface PageController<OUTPUT> {

    PageProvider<OUTPUT> getService();

    @GetMapping
    default ResponseEntity<Page<OUTPUT>> page(
        @RequestParam(name = "search", required = false) @Nullable String search,
        @RequestParam(name = "query", required = false) @Nullable String query,
        Pageable pageable,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> PeluwareToSpringAdapters.page(
            getService(),
            search,
            query,
            pageable
        )));
    }
}
