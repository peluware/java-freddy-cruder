package com.peluware.freddy.cruder.utils;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.text.Normalizer;

@UtilityClass
public final class StringUtils {

    public static @NotNull String toASCII(@NotNull String string) {
        try {
            var normalizedString = Normalizer.normalize(string, Normalizer.Form.NFD);
            return normalizedString
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .replaceAll("[^\\p{ASCII}]", "");
        } catch (Exception e) {
            throw new IllegalArgumentException("Error normalizando la cadena", e);
        }
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
