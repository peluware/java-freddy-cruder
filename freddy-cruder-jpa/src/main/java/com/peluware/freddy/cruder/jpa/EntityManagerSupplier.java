package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityManager;

/**
 * Provides access to a JPA {@link EntityManager}.
 * <p>
 * Classes implementing this interface expose a method to obtain the
 * {@link EntityManager} instance used for database operations.
 * </p>
 */
public interface EntityManagerSupplier {

    /**
     * Returns the {@link EntityManager} instance.
     *
     * @return the entity manager
     */
    EntityManager getEntityManager();
}
