package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.metamodel.Metamodel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JpaUtils {

    private JpaUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<Class<?>, String> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    public static String getIdFieldName(Metamodel metamodel, Class<?> entityClass) {
        return ID_FIELD_CACHE.computeIfAbsent(entityClass, cls -> {
            var et = metamodel.entity(cls);
            return et.getId(et.getIdType().getJavaType()).getName();
        });
    }

    /**
     * Executes {@code function} within a transaction, honoring any existing external
     * transaction (Spring, JTA, or container-managed).
     *
     * <p>
     * If the entity manager is already joined to an active transaction
     * ({@link EntityManager#isJoinedToTransaction()} returns {@code true}),
     * the function executes directly without opening a new transaction —
     * the caller's transaction boundary is reused.
     * </p>
     *
     * <p>
     * If no transaction is active, the method attempts to start and manage a
     * resource-local {@link EntityTransaction}. If the entity manager is
     * JTA-managed (e.g. in a Jakarta EE container or Spring with JTA), calling
     * {@link EntityManager#getTransaction()} is not permitted and will throw
     * {@link IllegalStateException} — in that case the function executes directly,
     * delegating transaction management to the JTA coordinator.
     * </p>
     *
     * @param em       the entity manager whose transaction context is checked
     * @param function the operation to execute
     * @param <T>      the return type
     * @return the result of the function
     */
    public static <T> T requireTransaction(EntityManager em, Supplier<T> function) {
        if (em.isJoinedToTransaction()) {
            return function.get();
        }
        try {
            return requireTransaction(em.getTransaction(), function);
        } catch (IllegalStateException e) {
            // JTA-managed EntityManager — transaction boundary is the container's responsibility
            return function.get();
        }
    }

    public static <T> T requireTransaction(EntityTransaction transaction, Supplier<T> function) {
        boolean weStartedIt = !transaction.isActive();

        try {
            if (weStartedIt) {
                transaction.begin();
            }

            T result = function.get();

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
