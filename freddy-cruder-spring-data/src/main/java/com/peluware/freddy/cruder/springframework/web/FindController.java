package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


public interface FindController<ID, OUTPUT> {

    SpringCrudProvider<ID, ?, OUTPUT> getService();

    @GetMapping("/{id}")
    default ResponseEntity<@NonNull OUTPUT> find(@PathVariable ID id) {
        return ResponseEntity.ok(getService().find(id));
    }
}
