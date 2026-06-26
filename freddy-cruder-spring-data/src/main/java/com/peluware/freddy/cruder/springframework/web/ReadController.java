package com.peluware.freddy.cruder.springframework.web;


import com.peluware.freddy.cruder.ReadProvider;


public interface ReadController<ID, OUTPUT> extends
        PageController<OUTPUT>,
        FindController<ID, OUTPUT>,
        CountController,
        ExistsController<ID> {

    @Override
    ReadProvider<ID, OUTPUT> getService();
}
