package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringOwnedCrudProvider;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface OwnedDeleteController<OWNER_ID, ID> {

    SpringOwnedCrudProvider<OWNER_ID, ID, ?, ?> getService();

    @DeleteMapping("/{id}")
    default ResponseEntity<@NonNull Void> delete(
            @PathVariable OWNER_ID ownerId,
            @PathVariable ID id,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        getService().delete(ownerId, id, options);
        return ResponseEntity.ok().build();
    }
}