package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import com.peluware.freddy.cruder.springframework.SpringOwnedCrudProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface OwnedDeleteController<OWNER_ID, ID> {

    SpringOwnedCrudProvider<OWNER_ID, ID, ?, ?> getService();

    @DeleteMapping("/{id}")
    default ResponseEntity<Void> delete(
            @PathVariable("ownerId") OWNER_ID ownerId,
            @PathVariable("id") ID id,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        CrudContext.run(options, () -> getService().delete(ownerId, id));
        return ResponseEntity.noContent().build();
    }
}