package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;


public interface DeleteController<ID> {

    SpringCrudProvider<ID, ?, ?> getService();

    @DeleteMapping("/{id}")
    default ResponseEntity<@NonNull Void> delete(
            @PathVariable ID id,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        getService().delete(id, options);
        return ResponseEntity.ok().build();
    }
}
