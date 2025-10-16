package com.peluware.freddy.cruder.reactive.mutiny.events;

import io.smallrye.mutiny.Uni;

/**
 * Reactive event hooks for write operations.
 * <p>
 * Each method returns a {@link Uni<Void>} to allow chaining in reactive flows.
 * The {@link #eachEntity(Object)} method can remain synchronous.
 *
 * @param <E> Entity type
 * @param <D> DTO type
 */
public interface MutinyWriteEvents<E, D> {

    MutinyWriteEvents<?, ?> DEFAULT = new MutinyWriteEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E, D> MutinyWriteEvents<E, D> getDefault() {
        return (MutinyWriteEvents<E, D>) DEFAULT;
    }

    default Uni<Void> onBeforeCreate(D dto, E entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onAfterCreate(D dto, E entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onBeforeUpdate(D dto, E entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onAfterUpdate(D dto, E entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onBeforeDelete(E entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onAfterDelete(E entity) {
        return Uni.createFrom().voidItem();
    }

    default void eachEntity(E entity) {
        // synchronous, can remain empty
    }
}
