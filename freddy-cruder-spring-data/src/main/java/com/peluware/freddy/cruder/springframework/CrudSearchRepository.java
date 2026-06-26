package com.peluware.freddy.cruder.springframework;

import org.springframework.data.repository.CrudRepository;

/**
 * Convenience interface that combines {@link CrudRepository} and {@link SearchRepository}
 * into a single contract, eliminating the need to pass them as separate constructor arguments
 * to {@link SpringRepositoryCrudProvider}.
 *
 * @param <ENTITY> the entity type
 * @param <ID>     the entity identifier type
 */
public interface CrudSearchRepository<ENTITY, ID> extends CrudRepository<ENTITY, ID>, SearchRepository<ENTITY> {
}
