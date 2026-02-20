package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


public interface UpdateController<ID, INPUT, OUTPUT> {

    SpringCrudProvider<ID, INPUT, OUTPUT> getService();

    @PutMapping("/{id}")
    default ResponseEntity<@NonNull OUTPUT> update(@PathVariable ID id, @RequestBody INPUT input) {
        return ResponseEntity.ok(getService().update(id, input));
    }
}
