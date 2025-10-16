package com.peluware.freddy.cruder.reactive;

/**
 * Combines {@link FlowReadProvider} and {@link FlowWriteProvider}
 * to define the full CRUD contract using {@link java.util.concurrent.Flow}.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface FlowCrudProvider<E, D, ID> extends FlowReadProvider<E, ID>, FlowWriteProvider<E, D, ID> {
    // Inherits all Flow-based CRUD operations
}
