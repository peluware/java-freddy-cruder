package com.peluware.freddy.cruder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Provides creation of a new resource from an input DTO.
 *
 * @param <INPUT>  the input DTO type used to create the resource
 * @param <OUTPUT> the output DTO or projection type returned to the consumer
 */
@FunctionalInterface
public interface CreateProvider<INPUT, OUTPUT> {

    /**
     * Creates a new resource using the provided input DTO.
     *
     * @param input input DTO containing creation data
     * @return the newly created resource mapped to its output representation
     */
    OUTPUT create(@NotNull @Valid INPUT input);
}
