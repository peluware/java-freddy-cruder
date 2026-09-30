package com.peluware.freddy.cruder.jpa;

import java.util.Objects;

/**
 * The single {@link JpaTransactionRunner} every {@link JpaCrudProvider} in the JVM uses.
 */
public final class JpaTransactionRunners {

    private static volatile JpaTransactionRunner current = JpaTransactionRunner.resourceLocal();

    private JpaTransactionRunners() {
    }

    /**
     * @return the currently installed runner, or {@link JpaTransactionRunner#resourceLocal()} if
     * nothing called {@link #install}
     */
    public static JpaTransactionRunner current() {
        return current;
    }

    /**
     * Replaces the runner returned by {@link #current()} from now on.
     */
    public static void install(JpaTransactionRunner runner) {
        current = Objects.requireNonNull(runner, "runner must not be null");
    }
}
