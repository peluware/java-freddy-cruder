package com.peluware.freddy.cruder.events;


public interface WriteEvents<E, D> {

    WriteEvents<?, ?> DEFAULT = new WriteEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E, D> WriteEvents<E, D> getDefault() {
        return (WriteEvents<E, D>) DEFAULT;
    }

    default void onBeforeCreate(D dto, E entity) {
    }

    default void onBeforeUpdate(D dto, E entity) {
    }

    default void onBeforeDelete(E entity) {
    }

    default void onAfterCreate(D dto, E entity) {
    }

    default void onAfterUpdate(D dto, E entity) {
    }

    default void onAfterDelete(E entity) {
    }

    default void eachEntity(E entity) {

    }
}
