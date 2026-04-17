package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.OwnedEntityCrudProvider;

public abstract class SpringOwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> extends OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> implements SpringOwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> {

    public SpringOwnedEntityCrudProvider(Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
    }

    public SpringOwnedEntityCrudProvider(Class<ENTITY> entityClass) {
        this(entityClass, EntityCrudEvents.getDefault());
    }
}