package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.CrudOptions;
import org.springframework.util.MultiValueMap;

import java.time.Instant;
import java.util.*;

public final class SpringCrudOptions implements CrudOptions {

    private final MultiValueMap<String, String> parameters;

    public SpringCrudOptions(MultiValueMap<String, String> parameters) {
        this.parameters = Objects.requireNonNull(parameters, "Parameters map cannot be null");
    }

    public static CrudOptions of(MultiValueMap<String, String> parameters) {
        return new SpringCrudOptions(parameters);
    }

    // --------------------------------------------------
    // Básico
    // --------------------------------------------------

    @Override
    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    @Override
    public boolean contains(String key) {
        return parameters.containsKey(key);
    }

    @Override
    public Set<String> keys() {
        return parameters.keySet();
    }

    // --------------------------------------------------
    // Genérico
    // --------------------------------------------------

    @Override
    public <T> T get(String key, Class<T> type) {
        String value = parameters.getFirst(key);
        if (value == null) return null;
        return convert(value, type, key);
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
        return get(key, enumType);
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
        List<String> raw = parameters.get(key);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        List<T> result = new ArrayList<>(raw.size());
        for (String value : raw) {
            result.add(convert(value, elementType, key));
        }

        return List.copyOf(result);
    }

    @Override
    public <T> List<T> getAllOrDefault(String key, Class<T> elementType, List<T> defaultValue) {
        List<T> list = getAll(key, elementType);
        return list.isEmpty() ? defaultValue : list;
    }

    // --------------------------------------------------
    // Conversión simple
    // --------------------------------------------------

    @SuppressWarnings("unchecked")
    private <T> T convert(String value, Class<T> type, String key) {

        try {
            if (type == String.class) {
                return (T) value;
            }

            if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(value);
            }

            if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(value);
            }

            if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(value);
            }

            if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(value);
            }

            if (type == UUID.class) {
                return (T) UUID.fromString(value);
            }

            if (type == Instant.class) {
                return (T) Instant.parse(value);
            }

            if (type == Date.class) {
                return (T) Date.from(Instant.parse(value));
            }

            if (Enum.class.isAssignableFrom(type)) {
                return (T) Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), value);
            }

        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid value for option '" + key + "': " + value);
        }

        throw new IllegalArgumentException("Unsupported option type: " + type.getSimpleName());
    }
}