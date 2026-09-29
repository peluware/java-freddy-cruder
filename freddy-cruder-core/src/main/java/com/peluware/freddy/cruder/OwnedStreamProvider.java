package com.peluware.freddy.cruder;

import com.peluware.domain.Sort;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.stream.Stream;

/**
 * Provides an unpaginated, lazily-read listing of resources scoped to a given owner.
 *
 * <p>Same intent as {@link StreamProvider}, scoped to an owner. Not composed into
 * {@link OwnedReadProvider}, for the same reason.</p>
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <OUTPUT>   the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface OwnedStreamProvider<OWNER_ID, OUTPUT> {

    /**
     * @param ownerId unique identifier of the owning resource
     * @param search  optional text-based search (may be {@code null})
     * @param query   additional filtering expression, may be {@code null}
     * @param sort    sorting configuration, or {@code null} for unsorted results
     * @return every matching resource belonging to the owner, lazily; close it once done
     */
    Stream<OUTPUT> stream(@NotNull OWNER_ID ownerId, @Nullable String search, @Nullable String query, Sort sort);
}
