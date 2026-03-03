package com.peluware.freddy.cruder;

import java.util.*;

public final class DefaultCrudOptions implements CrudOptions {

    private final Map<String, List<Object>> values;

    public DefaultCrudOptions(Map<String, ?> source) {

        Objects.requireNonNull(source, "Options source map must not be null");

        Map<String, List<Object>> copy = new HashMap<>();

        source.forEach((key, value) -> {
            if (value instanceof Collection<?> collection) {
                copy.put(key, List.copyOf(collection));
            } else if (value != null) {
                copy.put(key, List.of(value));
            } else {
                copy.put(key, List.of());
            }
        });

        this.values = Map.copyOf(copy);
    }

    public static CrudOptions empty() {
        return new DefaultCrudOptions(Map.of());
    }

    // --------------------------------------------------
    // Básico
    // --------------------------------------------------

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public Set<String> keys() {
        return values.keySet();
    }

    // --------------------------------------------------
    // Genérico tipado
    // --------------------------------------------------

    @Override
    public <T> T get(String key, Class<T> type) {
        List<Object> list = values.get(key);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return cast(list.getFirst(), type, key);
    }

    @Override
    public <T> T getOrDefault(String key, Class<T> type, T defaultValue) {
        T value = get(key, type);
        return value != null ? value : defaultValue;
    }

    @Override
    public <T> Optional<T> getOptional(String key, Class<T> type) {
        return Optional.ofNullable(get(key, type));
    }

    @Override
    public <T> T require(String key, Class<T> type) {
        T value = get(key, type);
        if (value == null) {
            throw new IllegalArgumentException("Required option '" + key + "' not present");
        }
        return value;
    }

    // --------------------------------------------------
    // Enums
    // --------------------------------------------------

    @Override
    public <E extends Enum<E>> E getEnum(String key, Class<E> enumType) {
        Object raw = get(key, Object.class);
        if (raw == null) return null;

        if (enumType.isInstance(raw)) {
            return enumType.cast(raw);
        }

        if (raw instanceof String str) {
            return Enum.valueOf(enumType, str);
        }

        throw new IllegalArgumentException("Option '" + key + "' cannot be converted to enum " + enumType.getSimpleName());
    }

    @Override
    public <E extends Enum<E>> E getEnumOrDefault(String key, Class<E> enumType, E defaultValue) {
        E value = getEnum(key, enumType);
        return value != null ? value : defaultValue;
    }

    // --------------------------------------------------
    // Multi-value
    // --------------------------------------------------

    @Override
    public <T> List<T> getAll(String key, Class<T> elementType) {
        List<Object> list = values.get(key);
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        List<T> result = new ArrayList<>(list.size());
        for (Object value : list) {
            result.add(cast(value, elementType, key));
        }

        return List.copyOf(result);
    }

    @Override
    public <T> List<T> getAllOrDefault(String key, Class<T> elementType, List<T> defaultValue) {
        List<T> list = getAll(key, elementType);
        return list.isEmpty() ? defaultValue : list;
    }

    // --------------------------------------------------
    // Internal casting
    // --------------------------------------------------

    private <T> T cast(Object value, Class<T> type, String key) {
        if (value == null) return null;

        if (!type.isInstance(value)) {
            throw new IllegalArgumentException("Option '" + key + "' is not of type " + type.getSimpleName());
        }

        return type.cast(value);
    }
}