package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.OwnedCrudProvider;

public interface OwnedCrudController<OWNER_ID, ID, INPUT, OUTPUT> extends
        OwnedWriteController<OWNER_ID, ID, INPUT, OUTPUT>,
        OwnedReadController<OWNER_ID, ID, OUTPUT> {

    @Override
    OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> getService();
}