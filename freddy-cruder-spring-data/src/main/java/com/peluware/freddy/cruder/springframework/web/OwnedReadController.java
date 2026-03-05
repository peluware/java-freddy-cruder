package com.peluware.freddy.cruder.springframework.web;

import com.peluware.freddy.cruder.springframework.SpringOwnedCrudProvider;

public interface OwnedReadController<OWNER_ID, ID, OUTPUT> extends
        OwnedPageController<OWNER_ID, ID, OUTPUT>,
        OwnedFindController<OWNER_ID, ID, OUTPUT>,
        OwnedCountController<OWNER_ID>,
        OwnedExistsController<OWNER_ID, ID> {

    @Override
    SpringOwnedCrudProvider<OWNER_ID, ID, ?, OUTPUT> getService();
}