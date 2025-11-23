package com.peluware.freddy.cruder.reactive.mutiny;


import com.peluware.domain.Page;
import io.smallrye.mutiny.Uni;

public interface MutinyCrudEvents<ENTITY, ID, INPUT> {

    MutinyCrudEvents<?, ?, ?> DEFAULT = new MutinyCrudEvents<>() {
    };

    @SuppressWarnings("unchecked")
    static <E, D, ID> MutinyCrudEvents<E, D, ID> getDefault() {
        return (MutinyCrudEvents<E, D, ID>) DEFAULT;
    }

    default Uni<Void> onFind(ENTITY entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onCount(long count) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onExists(boolean exists, ID id) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onPage(Page<ENTITY> page) {
        return Uni.createFrom().voidItem();
    }


    default Uni<Void> onBeforeCreate(INPUT input, ENTITY entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onAfterCreate(INPUT input, ENTITY entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onBeforeUpdate(INPUT input, ENTITY entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onAfterUpdate(INPUT input, ENTITY entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onBeforeDelete(ENTITY entity) {
        return Uni.createFrom().voidItem();
    }

    default Uni<Void> onAfterDelete(ENTITY entity) {
        return Uni.createFrom().voidItem();
    }

    default void eachEntity(ENTITY ENTITY) {
    }
}
