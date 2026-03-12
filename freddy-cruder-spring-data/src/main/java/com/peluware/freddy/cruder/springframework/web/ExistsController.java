package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

public interface ExistsController<ID> {

    SpringCrudProvider<ID, ?, ?> getService();

    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    default ResponseEntity<@NonNull Boolean> exists(
            @PathVariable("id") ID id,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> getService().exists(id)));
    }
}
