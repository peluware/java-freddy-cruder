package com.peluware.freddy.cruder.hibernate.reactive;

import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Supplier of Hibernate Reactive {@link Mutiny.SessionFactory}
 *
 * @see Mutiny.SessionFactory
 * @see HibernateReadProvider
 * @see HibernateWriteProvider
 */
public interface HibernateSessionFactorySupplier {

    /**
     * Gets the Hibernate Reactive {@link Mutiny.SessionFactory}
     *
     * @return the session factory
     * @see Mutiny.SessionFactory
     * @see HibernateReadProvider
     * @see HibernateWriteProvider
     */
    Mutiny.SessionFactory getSessionFactory();
}
