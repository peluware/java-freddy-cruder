package com.peluware.freddy.cruder.reactive.mutiny;


import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.reactive.mutiny.events.MutinyCrudEvents;

/**
 * Support interface for full reactive CRUD operations.
 * <p>
 * Combines {@link MutinyReadProviderSupport} and {@link MutinyWriteProviderSupport},
 * providing default behaviors for events, pre/post processing hooks, and reactive transaction management.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface MutinyCrudProviderSupport<E, D, ID>
        extends MutinyReadProviderSupport<E, ID>,
        MutinyWriteProviderSupport<E, D, ID>,
        MutinyCrudProvider<E, D, ID> {

    /**
     * Returns the reactive CRUD event handler.
     * <p>
     * Default implementation returns a no-op handler.
     *
     * @return the reactive CRUD events instance
     */
    @Protected
    default MutinyCrudEvents<E, D, ID> getEvents() {
        return MutinyCrudEvents.getDefault();
    }
}
