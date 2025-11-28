package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.CrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


public interface ExistsController<ID> {

    CrudProvider<ID, ?, ?> getService();

    @GetMapping("/exists")
    default ResponseEntity<@NonNull Boolean> exists(@RequestParam ID id) {
        return ResponseEntity.ok(getService().exists(id));
    }
}
