package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.OwnedUpdateProvider;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

public interface OwnedUpdateController<OWNER_ID, ID, INPUT, OUTPUT> {

    OwnedUpdateProvider<OWNER_ID, ID, INPUT, OUTPUT> getService();

    @PutMapping("/{id}")
    default ResponseEntity<OUTPUT> update(
            @PathVariable("ownerId") OWNER_ID ownerId,
            @PathVariable("id") ID id,
            @RequestBody INPUT input,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> getService().update(ownerId, id, input)));
    }
}