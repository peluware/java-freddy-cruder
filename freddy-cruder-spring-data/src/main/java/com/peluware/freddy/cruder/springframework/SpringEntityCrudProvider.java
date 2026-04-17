package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.EntityCrudProvider;

public abstract class SpringEntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> implements SpringCrudProvider<ID, INPUT, OUTPUT> {

    public SpringEntityCrudProvider(Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
    }

    public SpringEntityCrudProvider(Class<ENTITY> entityClass) {
        this(entityClass, EntityCrudEvents.getDefault());
    }
}
