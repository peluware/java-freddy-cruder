package com.peluware.freddy.cruder.reactive.mutiny.events;


/**
 * Reactive event hooks for full CRUD operations.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type
 * @param <ID> Identifier type
 */
public interface MutinyCrudEvents<E, D, ID> extends MutinyReadEvents<E, ID>, MutinyWriteEvents<E, D> {

    MutinyCrudEvents<?, ?, ?> DEFAULT = new MutinyCrudEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E, D, ID> MutinyCrudEvents<E, D, ID> getDefault() {
        return (MutinyCrudEvents<E, D, ID>) DEFAULT;
    }

    @Override
    default void eachEntity(E entity) {
    }
}
