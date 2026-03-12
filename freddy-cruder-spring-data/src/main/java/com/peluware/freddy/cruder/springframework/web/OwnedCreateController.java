package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringOwnedCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface OwnedCreateController<OWNER_ID, INPUT, OUTPUT> {

    SpringOwnedCrudProvider<OWNER_ID, ?, INPUT, OUTPUT> getService();

    @PostMapping
    default ResponseEntity<@NonNull OUTPUT> create(
            @PathVariable("ownerId") OWNER_ID ownerId,
            @RequestBody INPUT input,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> getService().create(ownerId, input)));
    }
}