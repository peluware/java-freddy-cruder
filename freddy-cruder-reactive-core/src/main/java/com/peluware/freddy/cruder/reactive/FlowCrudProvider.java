package com.peluware.freddy.cruder.reactive;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.concurrent.Flow;


/**
 * Proveedor CRUD reactivo genérico usando la API Java Flow, diseñado para desacoplar
 * la lógica de aplicación de los detalles de persistencia. Las implementaciones definen
 * cómo se crean, recuperan, actualizan, eliminan y listan entidades de manera reactiva,
 * sin imponer restricciones sobre el framework o la tecnología subyacente.
 *
 * <p>
 * Este proveedor es adecuado para escenarios donde se requiere backpressure y flujos
 * reactivos. Permite emitir resultados como {@link java.util.concurrent.Flow.Publisher}
 * manteniendo la consistencia y seguridad de tipos.
 * </p>
 *
 * @param <ID>     Tipo del identificador único del recurso.
 * @param <INPUT>  Tipo del DTO de entrada usado para crear o actualizar recursos.
 * @param <OUTPUT> Tipo del DTO de salida devuelto al consumidor.
 */
public interface FlowCrudProvider<OUTPUT, INPUT, ID> {


    /**
     * Retrieves a paginated list of entities based on optional search criteria and query.
     *
     * @param search     optional search string (normalized before use)
     * @param pagination pagination settings, may be {@code null} for unpaginated
     * @param sort       sorting options, may be {@code null} for unsorted
     * @param query      additional query criteria, may be {@code null}
     * @return a {@link Flow.Publisher} emitting a single {@link Page} of entities
     */
    Flow.Publisher<Page<OUTPUT>> page(String search, Pagination pagination, Sort sort, String query);

    /**
     * Finds an entity by its identifier.
     *
     * @param id the identifier of the entity
     * @return a {@link Flow.Publisher} emitting the found entity, or completing empty if not found
     */
    Flow.Publisher<OUTPUT> find(@NotNull ID id);

    /**
     * Counts the number of entities matching the given search and query criteria.
     *
     * @param search optional search string (normalized before use)
     * @param query  additional query criteria, may be {@code null}
     * @return a {@link Flow.Publisher} emitting a single {@link Long} with the number of matches
     */
    Flow.Publisher<Long> count(String search, String query);

    /**
     * Checks if an entity exists by its identifier.
     *
     * @param id the identifier of the entity
     * @return a {@link Flow.Publisher} emitting {@code true} if the entity exists, {@code false} otherwise
     */
    Flow.Publisher<Boolean> exists(@NotNull ID id);


    /**
     * Creates a new entity based on the given DTO.
     *
     * @param dto the data transfer object containing creation data
     * @return a {@link Flow.Publisher} emitting the newly created entity
     */
    Flow.Publisher<OUTPUT> create(@NotNull @Valid INPUT dto);

    /**
     * Updates an existing entity identified by the given ID using the provided DTO.
     *
     * @param id  the identifier of the entity to update
     * @param dto the data transfer object containing updated data
     * @return a {@link Flow.Publisher} emitting the updated entity,
     * or completing empty if not found
     */
    Flow.Publisher<OUTPUT> update(@NotNull ID id, @NotNull @Valid INPUT dto);

    /**
     * Deletes an entity identified by the given ID.
     *
     * @param id the identifier of the entity to delete
     * @return a {@link Flow.Publisher} completing when the entity is deleted,
     * or completing empty if not found
     */
    Flow.Publisher<Void> delete(@NotNull ID id);
}
