package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.CrudProvider;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Generic CRUD provider aligned with Spring Data abstractions.
 *
 * <p>
 * This contract is designed to integrate naturally with Spring,
 * leveraging {@link Page}, {@link Pageable}, and {@link Sort}
 * from Spring Data.
 * </p>
 *
 * @param <ID>     Type of the unique identifier.
 * @param <INPUT>  Input DTO used for create/update operations.
 * @param <OUTPUT> Output DTO returned to consumers.
 */
public interface SpringCrudProvider<ID, INPUT, OUTPUT> extends CrudProvider<ID, INPUT, OUTPUT> {

    /**
     * Retrieves a paginated list of resources using Spring's {@link Pageable}.
     *
     * @param search   optional text-based search (maybe {@code null})
     * @param query    optional additional filtering expression (maybe {@code null})
     * @param pageable Spring pagination and sorting abstraction (must not be {@code null})
     * @return a Spring {@link Page} containing results
     */
    default Page<OUTPUT> page(String search, String query, @NotNull Pageable pageable) {
        return SpringDataAdapters.toSpringDataCall(this, search, query, pageable);
    }
}