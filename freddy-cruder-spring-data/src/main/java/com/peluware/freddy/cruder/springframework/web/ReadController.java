package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.springframework.SpringCrudProvider;


public interface ReadController<ID, OUTPUT> extends
        PageController<ID, OUTPUT>,
        FindController<ID, OUTPUT>,
        CountController,
        ExistsController<ID> {

    @Override
    SpringCrudProvider<ID, ?, OUTPUT> getService();
}
