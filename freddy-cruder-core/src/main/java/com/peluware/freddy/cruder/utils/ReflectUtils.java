package com.peluware.freddy.cruder.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;

public final class ReflectUtils {

    private ReflectUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static <T> Class<?> resolveGenericType(
        Class<? extends T> source,
        Class<T> target,
        int index
    ) {
        var resolved = new HashMap<TypeVariable<?>, Type>();
        Class<?> current = source;
        while (current != null && current != Object.class) {
            var genericSuper = current.getGenericSuperclass();
            if (genericSuper instanceof ParameterizedType pt) {
                var raw = (Class<?>) pt.getRawType();
                var vars = raw.getTypeParameters();
                var actuals = pt.getActualTypeArguments();
                for (int i = 0; i < vars.length; i++) {
                    var actual = actuals[i];
                    while (actual instanceof TypeVariable<?> && resolved.containsKey(actual)) {
                        actual = resolved.get(actual);
                    }
                    resolved.put(vars[i], actual);
                }

                if (target.equals(raw)) {
                    Type resolvedType = actuals[index];
                    while (resolvedType instanceof TypeVariable<?> && resolved.containsKey(resolvedType)) {
                        resolvedType = resolved.get(resolvedType);
                    }

                    if (resolvedType instanceof ParameterizedType pt2) {
                        resolvedType = pt2.getRawType();
                    }

                    if (resolvedType instanceof Class<?> clazz) {
                        return clazz;
                    }

                    throw new IllegalStateException("Could not resolve generic type at index " + index);
                }
                current = raw;
            } else {
                current = current.getSuperclass();
            }
        }

        throw new IllegalStateException("Target generic superclass not found: " + target.getName());
    }
}