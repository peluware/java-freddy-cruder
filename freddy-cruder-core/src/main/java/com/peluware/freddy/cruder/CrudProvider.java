package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Proveedor genérico CRUD diseñado para desacoplar la lógica de aplicación
 * de los detalles de persistencia. Las implementaciones definen cómo se
 * crean, recuperan, actualizan, eliminan y listan entidades, sin imponer
 * restricciones sobre el framework o la tecnología subyacente.
 *
 * @param <ID>     Tipo del identificador único del recurso.
 * @param <INPUT>  Tipo del DTO de entrada usado para crear o actualizar recursos.
 * @param <OUTPUT> Tipo del DTO de salida devuelto al consumidor.
 */
public interface CrudProvider<ID, INPUT, OUTPUT> {

    /**
     * Retrieves a paginated list of resources based on optional search criteria and filtering expression.
     *
     * @param search     optional text-based search (may be {@code null})
     * @param query      additional filtering expression, may be {@code null}
     * @param pagination pagination settings, or {@code null} for unpaginated results
     * @param sort       sorting configuration, or {@code null} for unsorted results
     * @return a {@link Page} containing the paginated result set
     */
    Page<OUTPUT> page(String search, String query, Pagination pagination, Sort sort);

    /**
     * Finds a resource by its identifier.
     *
     * @param id unique identifier of the resource
     * @return the resource mapped to its output representation
     * @throws NotFoundEntityException if no resource exists with the given identifier
     */
    OUTPUT find(@NotNull ID id) throws NotFoundEntityException;

    /**
     * Counts the number of resources matching optional search and query filters.
     *
     * @param search optional text-based search (may be {@code null})
     * @param query  optional additional query expression (may be {@code null})
     * @return total number of resources matching the criteria
     */
    long count(String search, String query);

    /**
     * Checks whether a resource with the given identifier exists.
     *
     * @param id unique identifier of the resource
     * @return {@code true} if the resource exists, otherwise {@code false}
     */
    boolean exists(@NotNull ID id);

    /**
     * Creates a new resource using the provided input DTO.
     *
     * @param input input DTO containing creation data
     * @return the newly created resource mapped to its output representation
     */
    OUTPUT create(@NotNull @Valid INPUT input);

    /**
     * Updates an existing resource identified by the given ID.
     *
     * @param id    unique identifier of the resource to update
     * @param input input DTO containing updated data
     * @return the updated resource mapped to its output representation
     * @throws NotFoundEntityException if no resource exists with the given ID
     */
    OUTPUT update(@NotNull ID id, @NotNull @Valid INPUT input) throws NotFoundEntityException;

    /**
     * Deletes the resource identified by the given ID.
     *
     * @param id unique identifier of the resource to delete
     * @throws NotFoundEntityException if the resource does not exist
     */
    void delete(@NotNull ID id) throws NotFoundEntityException;
}
