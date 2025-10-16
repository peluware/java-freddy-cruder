package com.peluware.freddy.cruder;

/**
 * Combines {@link ReadProvider} and {@link WriteProvider} to define the full CRUD contract.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface CrudProvider<E, D, ID> extends ReadProvider<E, ID>, WriteProvider<E, D, ID> {
    // This interface inherits all read and write operations
}
