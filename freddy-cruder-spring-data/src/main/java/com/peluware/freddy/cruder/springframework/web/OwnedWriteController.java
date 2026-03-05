package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringOwnedCrudProvider;

public interface OwnedWriteController<OWNER_ID, ID, INPUT, OUTPUT> extends
        OwnedCreateController<OWNER_ID, INPUT, OUTPUT>,
        OwnedUpdateController<OWNER_ID, ID, INPUT, OUTPUT>,
        OwnedDeleteController<OWNER_ID, ID> {

    @Override
    SpringOwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> getService();
}