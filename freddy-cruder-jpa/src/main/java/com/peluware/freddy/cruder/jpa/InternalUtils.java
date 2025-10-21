package com.peluware.freddy.cruder.jpa;

import cz.jirutka.rsql.parser.RSQLParser;
import jakarta.persistence.Id;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
class InternalUtils {

    public static final RSQLParser DEFAULT_RSQL_PARSER = new RSQLParser();
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
}
