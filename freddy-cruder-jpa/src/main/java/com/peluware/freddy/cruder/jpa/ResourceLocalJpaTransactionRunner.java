package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityManager;

import java.util.function.Supplier;
import java.util.stream.Stream;

final class ResourceLocalJpaTransactionRunner implements JpaTransactionRunner {

    static final ResourceLocalJpaTransactionRunner INSTANCE = new ResourceLocalJpaTransactionRunner();

    private ResourceLocalJpaTransactionRunner() {
    }

    @Override
    public <T> T run(EntityManager entityManager, Supplier<T> function) {
        return JpaUtils.requireTransaction(entityManager, function);
    }

    @Override
    public <T> Stream<T> runStream(EntityManager entityManager, Supplier<Stream<T>> supplier) {
        return JpaUtils.requireTransactionStream(entityManager, supplier);
    }
}
