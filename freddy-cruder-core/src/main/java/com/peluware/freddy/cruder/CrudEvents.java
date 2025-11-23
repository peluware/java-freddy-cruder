package com.peluware.freddy.cruder;


import com.peluware.domain.Page;

public interface CrudEvents<ENTITY, ID, INPUT> {

    CrudEvents<?, ?, ?> DEFAULT = new CrudEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E, D, ID> CrudEvents<E, D, ID> getDefault() {
        return (CrudEvents<E, D, ID>) DEFAULT;
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