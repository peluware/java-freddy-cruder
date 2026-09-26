package com.peluware.freddy.cruder;


import com.peluware.domain.Page;

import java.util.ArrayList;
import java.util.Arrays;

public interface EntityCrudEvents<ENTITY, ID, INPUT> {

    EntityCrudEvents<?, ?, ?> DEFAULT = new EntityCrudEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E, D, ID> EntityCrudEvents<E, D, ID> getDefault() {
        return (EntityCrudEvents<E, D, ID>) DEFAULT;
    }

    @SafeVarargs
    static <ENTITY, ID, INPUT> EntityCrudEvents<ENTITY, ID, INPUT> of(EntityCrudEvents<ENTITY, ID, INPUT>... delegates) {
        return CompositeEntityCrudEvents.compose(Arrays.asList(delegates));
    }

    static <ENTITY, ID, INPUT> EntityCrudEvents<ENTITY, ID, INPUT> of(Iterable<? extends EntityCrudEvents<ENTITY, ID, INPUT>> delegates) {
        return CompositeEntityCrudEvents.compose(delegates);
    }

    default EntityCrudEvents<ENTITY, ID, INPUT> andAll(Iterable<? extends EntityCrudEvents<ENTITY, ID, INPUT>> others) {
        var all = new ArrayList<EntityCrudEvents<ENTITY, ID, INPUT>>();
        all.add(this);
        others.forEach(all::add);
        return CompositeEntityCrudEvents.compose(all);
    }

    default void onFind(ENTITY entity) {
    }

    default void onCount(long count) {
    }

    default void onExists(boolean exists, ID id) {
    }

    default void onPage(Page<ENTITY> page) {
    }

    default void onBeforeCreate(INPUT input, ENTITY entity) {
    }

    default void onBeforeUpdate(INPUT input, ENTITY entity) {
    }

    default void onBeforeDelete(ENTITY entity) {
    }

    default void onAfterCreate(INPUT input, ENTITY entity) {
    }

    default void onAfterUpdate(INPUT input, ENTITY entity) {
    }

    default void onAfterDelete(ENTITY entity) {
    }

    default void eachEntity(ENTITY entity) {
    }
}