package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.DeleteProvider;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;


public interface DeleteController<ID> {

    DeleteProvider<ID> getService();

    @DeleteMapping("/{id}")
    default ResponseEntity<Void> delete(
        @PathVariable("id") ID id,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        CrudContext.run(options, () -> getService().delete(id));
        return ResponseEntity.noContent().build();
    }
}
