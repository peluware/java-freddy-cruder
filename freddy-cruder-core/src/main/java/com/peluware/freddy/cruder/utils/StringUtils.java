package com.peluware.freddy.cruder.utils;

public final class StringUtils {

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isEmpty(String string) {
        return string == null || string.isEmpty();
    }

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    public static String normalize(String search) {
        return isBlank(search) ? null : search.trim();
    }
}
