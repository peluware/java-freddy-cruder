package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityManager;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Runs an operation against an {@link EntityManager}, opening a transaction around it when none is
 * already active. {@link JpaCrudProvider} delegates to whichever instance is installed in
 * {@link JpaTransactionRunners} instead of managing transactions itself.
 */
public interface JpaTransactionRunner {

    /**
     * Runs {@code function} within a transaction on {@code entityManager}, honoring one that's already
     * active or opening one otherwise.
     */
    <T> T run(EntityManager entityManager, Supplier<T> function);

    /**
     * Same as {@link #run}, for a {@link Stream} consumed lazily after this method returns: a
     * transaction opened for the call stays open until the returned stream is closed, not just until
     * this method returns.
     */
    <T> Stream<T> runStream(EntityManager entityManager, Supplier<Stream<T>> supplier);

    /**
     * Honors an already-active transaction; otherwise opens and manages a resource-local
     * {@link jakarta.persistence.EntityTransaction} directly on {@code entityManager}. See
     * {@link JpaUtils#requireTransaction} and {@link JpaUtils#requireTransactionStream}.
     */
    static JpaTransactionRunner resourceLocal() {
        return ResourceLocalJpaTransactionRunner.INSTANCE;
    }
}
