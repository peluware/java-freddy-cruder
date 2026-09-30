package com.peluware.freddy.cruder.springframework.jpa;

import com.peluware.freddy.cruder.jpa.JpaTransactionRunner;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link JpaTransactionRunner} backed by a Spring {@link PlatformTransactionManager}: honors an
 * {@code entityManager} that's already joined to a transaction, or opens a new Spring-managed
 * transaction on the calling thread otherwise.
 */
public class SpringJpaTransactionRunner implements JpaTransactionRunner {

    private final PlatformTransactionManager transactionManager;

    public SpringJpaTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public <T> T run(EntityManager entityManager, Supplier<T> function) {
        if (entityManager.isJoinedToTransaction()) {
            return function.get();
        }
        var template = new TransactionTemplate(transactionManager);
        return template.execute(_ -> function.get());
    }

    /**
     * {@inheritDoc} The transaction opened for the call, if any, commits when the returned stream
     * closes, or rolls back if {@code supplier} throws before returning.
     */
    @Override
    public <T> Stream<T> runStream(EntityManager entityManager, Supplier<Stream<T>> supplier) {
        if (entityManager.isJoinedToTransaction()) {
            return supplier.get();
        }

        var status = transactionManager.getTransaction(null);
        try {
            return supplier.get().onClose(() -> transactionManager.commit(status));
        } catch (RuntimeException e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}
