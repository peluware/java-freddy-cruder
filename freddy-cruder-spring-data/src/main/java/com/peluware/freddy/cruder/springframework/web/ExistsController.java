package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.CrudContext;
import com.peluware.freddy.cruder.ExistsProvider;
import com.peluware.freddy.cruder.springframework.SpringCrudOptions;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

public interface ExistsController<ID> {

    ExistsProvider<ID> getService();

    @RequestMapping(value = "/{id}", method = RequestMethod.HEAD)
    default ResponseEntity<Boolean> exists(
        @PathVariable("id") ID id,
        @RequestParam MultiValueMap<String, String> parameters
    ) {
        var options = SpringCrudOptions.of(parameters);
        return ResponseEntity.ok(CrudContext.call(options, () -> getService().exists(id)));
    }
}
