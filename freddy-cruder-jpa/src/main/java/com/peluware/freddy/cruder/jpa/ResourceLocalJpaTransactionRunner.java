package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Honors a transaction that is already active and otherwise opens and manages a resource-local
 * {@link EntityTransaction} directly on the {@link EntityManager}.
 */
final class ResourceLocalJpaTransactionRunner implements JpaTransactionRunner {

    static final ResourceLocalJpaTransactionRunner INSTANCE = new ResourceLocalJpaTransactionRunner();

    private ResourceLocalJpaTransactionRunner() {
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * If the entity manager is already joined to a transaction, {@code function} runs directly. If
     * not, a resource-local transaction is begun and committed around it, or rolled back if it
     * throws. A JTA-managed entity manager does not allow {@link EntityManager#getTransaction()};
     * there {@code function} also runs directly, leaving the boundary to the JTA coordinator.
     * </p>
     */
    @Override
    public <T> T run(EntityManager entityManager, Supplier<T> function) {
        if (entityManager.isJoinedToTransaction()) {
            return function.get();
        }

        var transaction = resourceLocalTransaction(entityManager);
        if (transaction == null) {
            return function.get();
        }

        return run(transaction, function);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * A transaction opened here is not committed when {@code supplier} returns, since the stream is
     * still unread, but when the returned stream is closed.
     * </p>
     */
    @Override
    public <T> Stream<T> runStream(EntityManager entityManager, Supplier<Stream<T>> supplier) {
        if (entityManager.isJoinedToTransaction()) {
            return supplier.get();
        }

        var transaction = resourceLocalTransaction(entityManager);
        if (transaction == null || transaction.isActive()) {
            return supplier.get();
        }

        transaction.begin();
        try {
            return supplier.get().onClose(() -> {
                if (transaction.isActive()) {
                    transaction.commit();
                }
            });
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * The resource-local transaction of {@code entityManager}, or {@code null} when it is
     * JTA-managed and does not allow asking for one.
     */
    private static @Nullable EntityTransaction resourceLocalTransaction(EntityManager entityManager) {
        try {
            return entityManager.getTransaction();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static <T> T run(EntityTransaction transaction, Supplier<T> function) {
        var weStartedIt = !transaction.isActive();

        try {
            if (weStartedIt) {
                transaction.begin();
            }

            var result = function.get();

            if (weStartedIt) {
                transaction.commit();
            }

            return result;
        } catch (RuntimeException e) {
            if (weStartedIt && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
}
