package com.peluware.freddy.cruder;

import com.peluware.domain.Sort;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Provides an unpaginated, lazily-read listing of resources with optional search and filter
 * criteria.
 *
 * <p>Same intent as {@link ListProvider}, but as a {@link Stream} instead of a
 * {@link java.util.List} — the shape a store-backed cursor needs to avoid loading every matching
 * resource into memory at once. The returned {@link Stream} must be fully consumed and closed
 * before this call's transaction ends; it does not outlive that call. Not composed into
 * {@link ReadProvider}, for the same reason {@link ListProvider} isn't.</p>
 *
 * @param <OUTPUT> the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface StreamProvider<OUTPUT> {

    /**
     * @param search optional text-based search (may be {@code null})
     * @param query  additional filtering expression, may be {@code null}
     * @param sort   sorting configuration, or {@code null} for unsorted results
     * @return every matching resource, lazily; close it once done
     */
    Stream<OUTPUT> stream(@Nullable String search, @Nullable String query, Sort sort);
}
