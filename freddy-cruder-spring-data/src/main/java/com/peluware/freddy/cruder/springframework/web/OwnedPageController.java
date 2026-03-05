package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringOwnedCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface OwnedPageController<OWNER_ID, ID, OUTPUT> {

    SpringOwnedCrudProvider<OWNER_ID, ID, ?, OUTPUT> getService();

    @GetMapping
    default ResponseEntity<@NonNull Page<@NonNull OUTPUT>> page(
            @PathVariable OWNER_ID ownerId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String query,
            Pageable pageable,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var filtered = new LinkedMultiValueMap<>(parameters);

        filtered.remove("search");
        filtered.remove("query");
        filtered.remove("page");
        filtered.remove("size");
        filtered.remove("sort");

        var options = SpringCrudOptions.of(filtered);

        return ResponseEntity.ok(getService().page(ownerId, search, query, pageable, options));
    }
}