package com.peluware.freddy.cruder.reactive.mutiny.events;

import com.peluware.domain.Page;
import io.smallrye.mutiny.Uni;

import java.util.List;

/**
 * Reactive event hooks for read operations.
 * <p>
 * Each method returns a {@link Uni<Void>} to allow chaining in reactive flows.
 * The {@link #eachEntity(Object)} method can remain synchronous.
 *
 * @param <E>  Entity type
 * @param <ID> Identifier type
 */
public interface MutinyReadEvents<E, ID> {

    MutinyReadEvents<?, ?> DEFAULT = new MutinyReadEvents<>() {};

    @SuppressWarnings("unchecked")
    static <E, ID> MutinyReadEvents<E, ID> getDefault() {
        return (MutinyReadEvents<E, ID>) DEFAULT;
    }

    default Uni<Void> onFind(E entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onFind(List<E> entities, List<ID> ids) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onCount(long count) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onExists(boolean exists, ID id) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onPage(Page<E> page) {
        return Uni.createFrom().voidItem();
    }

    default void eachEntity(E entity) {
        // synchronous, can remain empty
    }
}
