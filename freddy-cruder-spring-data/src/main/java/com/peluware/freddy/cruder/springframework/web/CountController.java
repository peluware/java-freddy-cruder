package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


public interface CountController {

    SpringCrudProvider<?, ?, ?> getService();

    @GetMapping("/count")
    default ResponseEntity<@NonNull Long> count(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String query
    ) {
        return ResponseEntity.ok(getService().count(search, query));
    }
}
