package com.peluware.freddy.cruder;

import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.events.CrudEvents;


/**
 * Support implementation of {@link CrudProvider} providing common behavior.
 * <p>
 * This class resolves default behaviors such as event handling, transaction management,
 * and dispatching calls to specialized internal methods.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface CrudProviderSupport<E, D, ID> extends
        ReadProviderSupport<E, ID>,
        WriteProviderSupport<E, D, ID>,
        CrudProvider<E, D, ID> {

    /**
     * Factory method for creating the {@link CrudEvents} instance.
     * <p>
     * Subclasses can override to provide custom event handling.
     * </p>
     *
     * @return a new instance of {@link CrudEvents}
     */
    @Override
    @Protected
    default CrudEvents<E, D, ID> getEvents() {
        return CrudEvents.getDefault();
    }
}
