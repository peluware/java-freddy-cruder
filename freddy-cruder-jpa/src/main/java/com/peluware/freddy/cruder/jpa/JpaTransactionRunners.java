package com.peluware.freddy.cruder.jpa;

import java.util.Objects;

/**
 * The single {@link JpaTransactionRunner} every {@link JpaCrudProvider} in the JVM uses.
 */
public final class JpaTransactionRunners {

    private static volatile JpaTransactionRunner runner = JpaTransactionRunner.resourceLocal();

    private JpaTransactionRunners() {
    }

    /**
     * @return the currently installed runner, or {@link JpaTransactionRunner#resourceLocal()} if
     * nothing called {@link #install}
     */
    public static JpaTransactionRunner installed() {
        return runner;
    }

    /**
     * Replaces the runner returned by {@link #installed()} from now on.
     */
    public static void install(JpaTransactionRunner runner) {
        JpaTransactionRunners.runner = Objects.requireNonNull(runner, "runner must not be null");
    }
}
