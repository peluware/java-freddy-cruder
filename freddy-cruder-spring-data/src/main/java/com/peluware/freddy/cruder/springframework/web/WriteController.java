package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringCrudProvider;


public interface WriteController<ID, INPUT, OUTPUT> extends
        CreateController<INPUT, OUTPUT>,
        UpdateController<ID, INPUT, OUTPUT>,
        DeleteController<ID> {

    @Override
    SpringCrudProvider<ID, INPUT, OUTPUT> getService();
}
