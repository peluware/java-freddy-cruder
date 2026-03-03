package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


public interface ExistsController<ID> {

    SpringCrudProvider<ID, ?, ?> getService();

    @GetMapping("/{id}/exists")
    default ResponseEntity<@NonNull Boolean> exists(
            @PathVariable ID id,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(getService().exists(id, options));
    }
}
