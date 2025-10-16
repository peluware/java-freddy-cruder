package com.peluware.freddy.cruder.events;


public interface CrudEvents<E, D, ID> extends ReadEvents<E, ID>, WriteEvents<E, D> {

    CrudEvents<?, ?, ?> DEFAULT = new CrudEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E, D, ID> CrudEvents<E, D, ID> getDefault() {
        return (CrudEvents<E, D, ID>) DEFAULT;
    }

    @Override
    default void eachEntity(E entity) {
    }
}