package com.peluware.freddy.cruder.events;


import com.peluware.domain.Page;

public interface ReadEvents<E , ID> {

    ReadEvents<?, ?> DEFAULT = new ReadEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E , ID> ReadEvents<E, ID> getDefault() {
        return (ReadEvents<E, ID>) DEFAULT;
    }

    default void onFind(E entity) {
    }


    default void onFind(Iterable<E> entities, Iterable<ID> ids) {
    }

    default void onCount(long count) {
    }

    default void onExists(boolean exists, ID id) {
    }

    default void onPage(Page<E> page) {
    }

    default void eachEntity(E entity) {
    }
}
