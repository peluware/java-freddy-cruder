package com.peluware.freddy.cruder;

import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.events.WriteEvents;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.SneakyThrows;

import java.util.function.Supplier;

/**
 * Support implementation of {@link WriteProvider} providing common behavior.
 * <p>
 * This class resolves default behaviors such as event handling, transaction management,
 * and dispatching calls to specialized internal methods.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface WriteProviderSupport<E, D, ID> extends
        WriteProvider<E, D, ID>,
        EntityClassSupplier<E>,
        CrudProcessSupport {

    /**
     * Creates a new entity based on the given DTO.
     * <p>
     * Executes pre- and post-processing hooks, maps the DTO to the entity,
     * and fires CRUD events before and after creation.
     *
     * @param dto the data transfer object containing creation data
     * @return the newly created entity
     */
    @Override
    default E create(@NotNull @Valid D dto) {
        preProcess(CrudOperation.CREATE);

        var events = getEvents();
        return withTransaction(() -> {
            var entity = newEntity();

            mapEntity(dto, entity, true);
            events.onBeforeCreate(dto, entity);
            internalCreate(entity);
            events.onAfterCreate(dto, entity);
            events.eachEntity(entity);

            postProcess(CrudOperation.CREATE);
            return entity;
        });
    }

    /**
     * Updates an existing entity identified by the given ID using the provided DTO.
     * <p>
     * Executes pre- and post-processing hooks, maps the DTO to the entity,
     * and fires CRUD events before and after update.
     *
     * @param id  the identifier of the entity to update
     * @param dto the data transfer object containing updated data
     * @return the updated entity
     * @throws NotFoundEntityException if the entity with the given ID does not exist
     */
    @Override
    default E update(@NotNull ID id, @NotNull @Valid D dto) throws NotFoundEntityException {
        preProcess(CrudOperation.UPDATE);

        var events = getEvents();
        return withTransaction(() -> {
            var entity = internalFind(id);

            mapEntity(dto, entity, false);
            events.onBeforeUpdate(dto, entity);
            internalUpdate(entity);
            events.onAfterUpdate(dto, entity);
            events.eachEntity(entity);

            postProcess(CrudOperation.UPDATE);
            return entity;
        });
    }

    /**
     * Deletes an entity identified by the given ID.
     * <p>
     * Executes pre- and post-processing hooks and fires CRUD events before and after deletion.
     *
     * @param id the identifier of the entity to delete
     * @throws NotFoundEntityException if the entity with the given ID does not exist
     */
    @Override
    default void delete(@NotNull ID id) throws NotFoundEntityException {
        preProcess(CrudOperation.DELETE);

        withTransaction(() -> {
            var entity = internalFind(id);

            var events = getEvents();
            events.onBeforeDelete(entity);
            internalDelete(entity);
            events.onAfterDelete(entity);

            events.eachEntity(entity);

            postProcess(CrudOperation.DELETE);
            return null;
        });
    }

    /**
     * Provides the event handler instance for Write operations.
     * <p>
     * Default implementation returns a no-op event handler.
     * Subclasses may override to provide custom event handling logic.
     *
     * @return the event handler instance
     */
    @Protected
    default WriteEvents<E, D> getEvents() {
        return WriteEvents.getDefault();
    }

    @Protected
    void mapEntity(D dto, E entity, boolean isNew);

    @Protected
    E internalFind(ID id) throws NotFoundEntityException;

    @Protected
    void internalCreate(E entity);

    @Protected
    void internalUpdate(E entity);

    @Protected
    void internalDelete(E entity);

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
     * Utility method to run a function within a transaction context.
     * <p>
     * Subclasses can override to provide actual transaction management.
     * </p>
     *
     * @param function the function to execute within the transaction context
     * @param <T>      the return type of the function
     * @return the result of the function
     */
    default <T> T withTransaction(Supplier<T> function) {
        return function.get();
    }
}
