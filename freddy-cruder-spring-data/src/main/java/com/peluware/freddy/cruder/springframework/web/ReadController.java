package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.CrudProvider;


public interface ReadController<ID, OUTPUT> extends
        PageController<ID, OUTPUT>,
        FindController<ID, OUTPUT>,
        CountController,
        ExistsController<ID> {

    @Override
    CrudProvider<ID, ?, OUTPUT> getService();
}
