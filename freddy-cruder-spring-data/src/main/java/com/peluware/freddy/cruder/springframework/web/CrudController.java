package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringCrudProvider;

public interface CrudController<ID, INPUT, OUTPUT> extends
        WriteController<ID, INPUT, OUTPUT>,
        ReadController<ID, OUTPUT> {

    @Override
    SpringCrudProvider<ID, INPUT, OUTPUT> getService();
}
