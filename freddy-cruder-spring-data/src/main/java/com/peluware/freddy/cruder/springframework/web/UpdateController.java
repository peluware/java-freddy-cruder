package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;


public interface UpdateController<ID, INPUT, OUTPUT> {

    SpringCrudProvider<ID, INPUT, OUTPUT> getService();

    @PutMapping("/{id}")
    default ResponseEntity<@NonNull OUTPUT> update(
            @PathVariable("id") ID id,
            @RequestBody INPUT input,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> getService().update(id, input)));
    }
}
