package com.peluware.freddy.cruder.reactive.mutiny;


import com.peluware.freddy.cruder.CrudOperation;
import com.peluware.freddy.cruder.EntityClassSupplier;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.reactive.mutiny.events.MutinyWriteEvents;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;

import java.util.function.Supplier;

/**
 * Support interface for reactive write operations, providing default behaviors
 * such as event firing, mapping DTOs to entities, and reactive pre/post processing hooks.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface MutinyWriteProviderSupport<E, D, ID> extends
        MutinyWriteProvider<E, D, ID>,
        EntityClassSupplier<E>,
        MutinyCrudProcessSupport {

    @Override
    default Uni<E> create(@NotNull @Valid D dto) {
        var events = getEvents();
        return preProcess(CrudOperation.CREATE)
                .onItem().transformToUni(v -> withTransaction(() -> {
                    var entity = newEntity();
                    return mapEntity(dto, entity, true) // Uni<Void>
                            .onItem().call(vv -> events.onBeforeCreate(dto, entity))
                            .onItem().transformToUni(vv -> internalCreate(entity))
                            .onItem().call(e -> events.onAfterCreate(dto, entity))
                            .onItem().invoke(events::eachEntity)
                            .onItem().call(e -> postProcess(CrudOperation.CREATE));
                }));
    }

    @Override
    default Uni<E> update(@NotNull ID id, @NotNull @Valid D dto) {
        var events = getEvents();
        return preProcess(CrudOperation.UPDATE)
                .onItem().transformToUni(v -> withTransaction(() ->
                        internalFind(id)
                                .onItem().call(entity -> mapEntity(dto, entity, false))
                                .onItem().call(entity -> events.onBeforeUpdate(dto, entity))
                                .onItem().transformToUni(this::internalUpdate)
                                .onItem().call(entity -> events.onAfterUpdate(dto, entity))
                                .onItem().invoke(events::eachEntity)
                                .onItem().call(entity -> postProcess(CrudOperation.UPDATE))
                ));
    }

    @Override
    default Uni<Void> delete(@NotNull ID id) {
        var events = getEvents();
        return preProcess(CrudOperation.DELETE)
                .onItem().transformToUni(v -> withTransaction(() ->
                        internalFind(id)
                                .onItem().call(events::onBeforeDelete)
                                .onItem().transformToUni(this::internalDelete)
                                .onItem().call(events::onAfterDelete)
                                .onItem().invoke(events::eachEntity)
                                .onItem().call(entity -> postProcess(CrudOperation.DELETE))
                                .replaceWithVoid()
                ));
    }

    /**
     * Factory method for creating the {@link MutinyWriteEvents} instance.
     * <p>
     * Subclasses can override to provide custom event handling.
     * </p>
     */
    @Protected
    default MutinyWriteEvents<E, D> getEvents() {
        return MutinyWriteEvents.getDefault();
    }

    @Protected
    Uni<Void> mapEntity(D dto, E entity, boolean isNew);

    @Protected
    Uni<E> internalFind(ID id);

    @Protected
    Uni<E> internalCreate(E entity);

    @Protected
    Uni<E> internalUpdate(E entity);

    @Protected
    Uni<E> internalDelete(E entity);

    /**
     * Creates a new instance of the entity.
     * <p>
     * This method is invoked during the creation process before mapping the DTO in the {@link #create(D)} method.
     *
     * @return a new entity instance
     */
    @Protected
    @SneakyThrows
    default E newEntity() {
        return getEntityClass().getConstructor().newInstance();
    }

    /**
     * Runs a reactive function within a transaction context.
     * <p>
     * Default implementation just executes the supplier.
     *
     * @param function the function to execute
     * @param <T>      the return type
     * @return a {@link Uni} emitting the result
     */
    default <T> Uni<T> withTransaction(Supplier<Uni<T>> function) {
        return function.get();
    }
}
