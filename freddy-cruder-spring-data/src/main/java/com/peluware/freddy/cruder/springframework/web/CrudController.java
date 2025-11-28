package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.CrudProvider;

public interface CrudController<ID, INPUT, OUTPUT> extends
        WriteController<ID, INPUT, OUTPUT>,
        ReadController<ID, OUTPUT> {

    @Override
    CrudProvider<ID, INPUT, OUTPUT> getService();
}
