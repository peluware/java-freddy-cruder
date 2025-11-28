package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.CrudProvider;
import com.peluware.freddy.cruder.springframework.CrudProviderUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


public interface PageController<ID, OUTPUT> {

    CrudProvider<ID, ?, OUTPUT> getService();

    @GetMapping
    default ResponseEntity<@NonNull Page<@NonNull OUTPUT>> page(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String query,
            Pageable pageable
    ) {
        return ResponseEntity.ok(CrudProviderUtils.adaptSpringPage(
                getService(),
                search,
                query,
                pageable
        ));
    }
}
