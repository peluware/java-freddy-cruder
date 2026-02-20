package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


public interface CreateController<INPUT, OUTPUT> {

    SpringCrudProvider<?, INPUT, OUTPUT> getService();

    @PostMapping
    default ResponseEntity<@NonNull OUTPUT> create(@RequestBody INPUT input) {
        return ResponseEntity.ok(getService().create(input));
    }
}
