package com.peluware.freddy.cruder;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Proveedor genérico CRUD para subrecursos que pertenecen a un recurso padre.
 *
 * <p>
 * Extiende el contrato de {@link CrudProvider} añadiendo un identificador de
 * propietario ({@code OWNER_ID}) que actúa como contexto obligatorio en todas
 * las operaciones. Todas las acciones se ejecutan dentro del ámbito del
 * propietario: una entidad solo es visible, modificable o eliminable si
 * pertenece al dueño indicado.
 * </p>
 *
 * <p>
 * Casos de uso típicos incluyen relaciones del tipo:
 * <ul>
 *   <li>{@code /orders/{orderId}/items}</li>
 *   <li>{@code /departments/{deptId}/employees}</li>
 *   <li>{@code /projects/{projectId}/tasks}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Al igual que {@link CrudProvider}, todas las operaciones soportan
 * {@link CrudOptions} para extensibilidad, y disponen de una versión
 * simplificada que utiliza {@link CrudOptions#DEFAULT} por compatibilidad.
 * </p>
 *
 * @param <OWNER_ID> Tipo del identificador del recurso propietario.
 * @param <ID>       Tipo del identificador único del subrecurso.
 * @param <INPUT>    Tipo del DTO de entrada usado para crear o actualizar.
 * @param <OUTPUT>   Tipo del DTO de salida devuelto al consumidor.
 */
public interface OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> {

    // ==================================================
    // PAGE
    // ==================================================

    /**
     * Retrieves a paginated list of resources belonging to the given owner.
     *
     * @param ownerId    unique identifier of the owning resource
     * @param search     optional text-based search (may be {@code null})
     * @param query      additional filtering expression, may be {@code null}
     * @param pagination pagination settings, or {@code null} for unpaginated results
     * @param sort       sorting configuration, or {@code null} for unsorted results
     * @return a {@link Page} containing the paginated result set
     * @throws NotFoundException if the owner does not exist
     */
    default Page<OUTPUT> page(@NotNull OWNER_ID ownerId, String search, String query, Pagination pagination, Sort sort) throws NotFoundException {
        return page(ownerId, search, query, pagination, sort, CrudOptions.DEFAULT);
    }

    /**
     * Retrieves a paginated list of resources belonging to the given owner,
     * with additional execution options.
     *
     * @param ownerId    unique identifier of the owning resource
     * @param search     optional text-based search
     * @param query      additional filtering expression
     * @param pagination pagination settings
     * @param sort       sorting configuration
     * @param options    execution modifiers that may alter fetching,
     *                   filtering or performance behavior
     * @return a {@link Page} containing the paginated result set
     * @throws NotFoundException if the owner does not exist
     */
    Page<OUTPUT> page(@NotNull OWNER_ID ownerId, String search, String query, Pagination pagination, Sort sort, CrudOptions options) throws NotFoundException;

    // ==================================================
    // FIND
    // ==================================================

    /**
     * Finds a resource by its identifier within the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @return the resource mapped to its output representation
     * @throws NotFoundException if the owner or the resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    default OUTPUT find(@NotNull OWNER_ID ownerId, @NotNull ID id) throws NotFoundException {
        return find(ownerId, id, CrudOptions.DEFAULT);
    }

    /**
     * Finds a resource within the owner's scope, with execution options.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @param options execution modifiers (e.g. include relations, lock mode, etc.)
     * @return the resource mapped to its output representation
     * @throws NotFoundException if the owner or the resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    OUTPUT find(@NotNull OWNER_ID ownerId, @NotNull ID id, CrudOptions options) throws NotFoundException;

    // ==================================================
    // COUNT
    // ==================================================

    /**
     * Counts resources belonging to the given owner matching optional filters.
     *
     * @param ownerId unique identifier of the owning resource
     * @param search  optional text-based search (may be {@code null})
     * @param query   optional additional query expression (may be {@code null})
     * @return total number of resources matching the criteria
     * @throws NotFoundException if the owner does not exist
     */
    default long count(@NotNull OWNER_ID ownerId, String search, String query) throws NotFoundException {
        return count(ownerId, search, query, CrudOptions.DEFAULT);
    }

    /**
     * Counts resources within the owner's scope using additional execution options.
     *
     * @param ownerId unique identifier of the owning resource
     * @param search  optional search filter
     * @param query   optional query expression
     * @param options execution modifiers
     * @return total number of resources matching the criteria
     * @throws NotFoundException if the owner does not exist
     */
    long count(@NotNull OWNER_ID ownerId, String search, String query, CrudOptions options) throws NotFoundException;

    // ==================================================
    // EXISTS
    // ==================================================

    /**
     * Checks whether a resource with the given identifier exists within
     * the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @return {@code true} if the resource exists and belongs to the owner,
     * otherwise {@code false}
     */
    default boolean exists(@NotNull OWNER_ID ownerId, @NotNull ID id) {
        return exists(ownerId, id, CrudOptions.DEFAULT);
    }

    /**
     * Checks existence within the owner's scope with execution modifiers.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @param options execution modifiers
     * @return {@code true} if the resource exists and belongs to the owner
     */
    boolean exists(@NotNull OWNER_ID ownerId, @NotNull ID id, CrudOptions options);

    // ==================================================
    // CREATE
    // ==================================================

    /**
     * Creates a new resource under the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param input   input DTO containing creation data
     * @return the newly created resource mapped to its output representation
     * @throws NotFoundException if the owner does not exist
     */
    default OUTPUT create(@NotNull OWNER_ID ownerId, @NotNull @Valid INPUT input) throws NotFoundException {
        return create(ownerId, input, CrudOptions.DEFAULT);
    }

    /**
     * Creates a new resource under the given owner with execution modifiers.
     *
     * @param ownerId unique identifier of the owning resource
     * @param input   input DTO containing creation data
     * @param options execution modifiers (e.g. skip validation, audit flags)
     * @return the newly created resource
     * @throws NotFoundException if the owner does not exist
     */
    OUTPUT create(@NotNull OWNER_ID ownerId, @NotNull @Valid INPUT input, CrudOptions options) throws NotFoundException;

    // ==================================================
    // UPDATE
    // ==================================================

    /**
     * Updates an existing resource within the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource to update
     * @param input   input DTO containing updated data
     * @return the updated resource mapped to its output representation
     * @throws NotFoundException if the owner or the resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    default OUTPUT update(@NotNull OWNER_ID ownerId, @NotNull ID id, @NotNull @Valid INPUT input) throws NotFoundException {
        return update(ownerId, id, input, CrudOptions.DEFAULT);
    }

    /**
     * Updates a resource within the owner's scope using execution modifiers.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @param input   updated data
     * @param options execution modifiers
     * @return updated resource
     * @throws NotFoundException if the owner or resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    OUTPUT update(@NotNull OWNER_ID ownerId, @NotNull ID id, @NotNull @Valid INPUT input, CrudOptions options) throws NotFoundException;

    // ==================================================
    // DELETE
    // ==================================================

    /**
     * Deletes a resource within the scope of the given owner.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource to delete
     * @throws NotFoundException if the owner or the resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    default void delete(@NotNull OWNER_ID ownerId, @NotNull ID id) throws NotFoundException {
        delete(ownerId, id, CrudOptions.DEFAULT);
    }

    /**
     * Deletes a resource within the owner's scope using execution modifiers.
     *
     * @param ownerId unique identifier of the owning resource
     * @param id      unique identifier of the resource
     * @param options execution modifiers (e.g. soft delete, cascade control)
     * @throws NotFoundException if the owner or resource does not exist,
     *                           or if the resource does not belong to the owner
     */
    void delete(@NotNull OWNER_ID ownerId, @NotNull ID id, CrudOptions options) throws NotFoundException;
}
