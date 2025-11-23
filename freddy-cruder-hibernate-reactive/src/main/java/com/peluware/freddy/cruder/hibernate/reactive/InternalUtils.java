package com.peluware.freddy.cruder.hibernate.reactive;

import jakarta.persistence.Id;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InternalUtils {

    private InternalUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<Class<?>, String> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    static String getIdFieldName(Class<?> entityClass) {
        return ID_FIELD_CACHE.computeIfAbsent(entityClass, cls -> {
            for (var field : cls.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    return field.getName();
                }
            }
            throw new IllegalStateException("No field annotated with @Id found in " + cls.getName());
        });
    }

}
