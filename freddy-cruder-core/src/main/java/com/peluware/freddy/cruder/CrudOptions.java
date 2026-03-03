package com.peluware.freddy.cruder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface CrudOptions {

    CrudOptions DEFAULT = new DefaultCrudOptions(Map.of());


    // --------------------------------------------------
    // Básico
    // --------------------------------------------------

    boolean isEmpty();

    boolean contains(String key);

    Set<String> keys();


    // --------------------------------------------------
    // Acceso genérico tipado
    // --------------------------------------------------

    <T> T get(String key, Class<T> type);

    <T> T getOrDefault(String key, Class<T> type, T defaultValue);

    <T> Optional<T> getOptional(String key, Class<T> type);


    // --------------------------------------------------
    // Primitivos comunes (helpers)
    // --------------------------------------------------

    default String getString(String key) {
        return get(key, String.class);
    }

    default String getString(String key, String defaultValue) {
        return getOrDefault(key, String.class, defaultValue);
    }

    default Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }

    default boolean getBoolean(String key, boolean defaultValue) {
        return getOrDefault(key, Boolean.class, defaultValue);
    }

    default Integer getInt(String key) {
        return get(key, Integer.class);
    }

    default int getInt(String key, int defaultValue) {
        return getOrDefault(key, Integer.class, defaultValue);
    }

    default Long getLong(String key) {
        return get(key, Long.class);
    }

    default long getLong(String key, long defaultValue) {
        return getOrDefault(key, Long.class, defaultValue);
    }

    default Double getDouble(String key) {
        return get(key, Double.class);
    }

    default double getDouble(String key, double defaultValue) {
        return getOrDefault(key, Double.class, defaultValue);
    }


    // --------------------------------------------------
    // Enums
    // --------------------------------------------------

    <E extends Enum<E>> E getEnum(String key, Class<E> enumType);

    <E extends Enum<E>> E getEnumOrDefault(String key, Class<E> enumType, E defaultValue);


    <T> List<T> getAll(String key, Class<T> elementType);

    <T> List<T> getAllOrDefault(String key, Class<T> elementType, List<T> defaultValue);


    <T> T require(String key, Class<T> type) throws IllegalArgumentException;


}