package com.peluware.freddy.cruder.jpa;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Id;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class JpaUtils {

    private JpaUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<Class<?>, String> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    public static String getIdFieldName(Class<?> entityClass) {
        return ID_FIELD_CACHE.computeIfAbsent(entityClass, cls -> {
            for (var field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field.getName();
                }
            }
            throw new IllegalStateException("No field annotated with @Id found in " + cls.getName());
        });
    }

    public static <T> T withTransaction(EntityTransaction transaction, Supplier<T> function) {
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
