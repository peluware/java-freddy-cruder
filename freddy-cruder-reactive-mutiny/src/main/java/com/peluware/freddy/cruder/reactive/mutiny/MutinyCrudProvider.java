package com.peluware.freddy.cruder.reactive.mutiny;

/**
 * Combines {@link MutinyReadProvider} and {@link MutinyWriteProvider}
 * to define the full Mutiny CRUD contract.
 *
 * @param <E>  Entity type
 * @param <D>  DTO type used for create/update operations
 * @param <ID> Identifier type
 */
public interface MutinyCrudProvider<E, D, ID> extends MutinyReadProvider<E, ID>, MutinyWriteProvider<E, D, ID> {

}
