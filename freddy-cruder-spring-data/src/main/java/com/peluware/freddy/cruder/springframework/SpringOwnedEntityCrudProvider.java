package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.OwnedEntityCrudProvider;

public abstract class SpringOwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> extends OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> implements SpringOwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> {

    public SpringOwnedEntityCrudProvider(Class<ENTITY> entityClass) {
        super(entityClass);
    }
}