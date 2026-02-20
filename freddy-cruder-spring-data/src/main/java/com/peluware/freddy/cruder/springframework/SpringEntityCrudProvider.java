package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.EntityCrudProvider;

public abstract class SpringEntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> implements SpringCrudProvider<ID, INPUT, OUTPUT> {

    public SpringEntityCrudProvider(Class<ENTITY> entityClass) {
        super(entityClass);
    }
}
