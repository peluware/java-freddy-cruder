package com.peluware.freddy.cruder.utils;

import org.jspecify.annotations.Nullable;

public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isEmpty(@Nullable String string) {
        return string == null || string.isEmpty();
    }

    public static boolean isBlank(@Nullable String string) {
        return string == null || string.isBlank();
    }

    public static @Nullable String normalize(@Nullable String search) {
        return isBlank(search) ? null : search.trim();
    }
}
