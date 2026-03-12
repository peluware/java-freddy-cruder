package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


public interface FindController<ID, OUTPUT> {

    SpringCrudProvider<ID, ?, OUTPUT> getService();

    @GetMapping("/{id}")
    default ResponseEntity<@NonNull OUTPUT> find(
            @PathVariable("id") ID id,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> getService().find(id)));
    }
}
