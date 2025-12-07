package com.peluware.freddy.cruder.reactive.mutiny;


import com.peluware.domain.Page;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.function.Function;

/**
 * Utility class for working with {@link Page} objects in a reactive context using Mutiny.
 *
 * <p>
 * This class provides helper methods to transform the content of a {@link Page} while
 * preserving pagination and sorting metadata. All transformations are reactive and
 * non-blocking, returning {@link Uni} instances.
 * </p>
 *
 * <p>
 * Typical usage:
 * </p>
 * <pre>{@code
 * Uni<Page<OutputDto>> mappedPage = PageUtils.map(originalPage, entity -> mapEntityToDto(entity));
 * }</pre>
 *
 * <p>
 * The resulting {@link Page} retains the original pagination, sorting, and total elements,
 * ensuring the reactive workflow integrates seamlessly with your CRUD pipelines.
 * </p>
 */
public class PageUtils {

    private PageUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Maps the content of the given {@link Page} reactively using the provided mapper function.
     *
     * <p>
     * Each element of the original page is transformed to a {@link Uni} of the target type.
     * The resulting {@link Page} preserves pagination, sorting, and total elements.
     * </p>
     *
     * @param original the original page containing elements of type {@code T}
     * @param mapper   a function that converts each element of type {@code T} to a {@link Uni} of type {@code R}
     * @param <T>      the original element type
     * @param <R>      the target element type
     * @return a {@link Uni} emitting a new {@link Page} with elements of type {@code R}
     */
    public static <T, R> Uni<Page<R>> map(Page<T> original, Function<T, Uni<R>> mapper) {

        var content = original.getContent();

        if (content.isEmpty()) {
            // Si la página original está vacía, devolvemos una página vacía de R
            return Uni.createFrom().item(new Page<>(
                    List.of(),
                    original.getPagination(),
                    original.getSort(),
                    original.getTotalElements()
            ));
        }

        // Convertimos los T en una lista de Uni<R>
        var mappedUnis = content.stream()
                .map(mapper)
                .toList();

        return Uni.combine().all().unis(mappedUnis).with(results -> {
            @SuppressWarnings("unchecked")
            var mappedContent = (List<R>) results;

            // Construimos un nuevo Page<R> preservando metadata
            return new Page<>(
                    mappedContent,
                    original.getPagination(),
                    original.getSort(),
                    original.getTotalElements()
            );
        });
    }
}
