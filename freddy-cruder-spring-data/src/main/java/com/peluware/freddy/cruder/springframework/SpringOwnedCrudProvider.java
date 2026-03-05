package com.peluware.freddy.cruder.springframework;

import com.peluware.freddy.cruder.CrudOptions;
import com.peluware.freddy.cruder.OwnedCrudProvider;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Generic owned CRUD provider aligned with Spring Data abstractions.
 *
 * <p>
 * This contract extends {@link OwnedCrudProvider} integrating naturally with Spring,
 * leveraging {@link Page} and {@link Pageable} from Spring Data while preserving
 * the owner scope in every operation.
 * </p>
 *
 * @param <OWNER_ID> Type of the unique identifier of the owning resource.
 * @param <ID>       Type of the unique identifier of the owned resource.
 * @param <INPUT>    Input DTO used for create/update operations.
 * @param <OUTPUT>   Output DTO returned to consumers.
 */
public interface SpringOwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> extends OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> {

    /**
     * Retrieves a paginated list of resources belonging to the given owner,
     * using Spring's {@link Pageable}.
     *
     * @param ownerId  unique identifier of the owning resource
     * @param search   optional text-based search (may be {@code null})
     * @param query    optional additional filtering expression (may be {@code null})
     * @param pageable Spring pagination and sorting abstraction (must not be {@code null})
     * @param options  execution modifiers
     * @return a Spring {@link Page} containing results scoped to the owner
     * @throws com.peluware.freddy.cruder.NotFoundException if the owner does not exist
     */
    default Page<OUTPUT> page(@NotNull OWNER_ID ownerId, String search, String query, @NotNull Pageable pageable, CrudOptions options) {
        return SpringDataAdapters.toSpringOwnedDataCall(this, ownerId, search, query, pageable, options);
    }
}