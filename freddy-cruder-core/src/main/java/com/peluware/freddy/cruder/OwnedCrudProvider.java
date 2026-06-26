package com.peluware.freddy.cruder;

/**
 * Generic CRUD provider for sub-resources that belong to a parent resource.
 *
 * <p>
 * Extends the contract of {@link CrudProvider} by adding an owner identifier
 * ({@code OWNER_ID}) that acts as mandatory context for all operations. Every action
 * is executed within the owner's scope: an entity is only visible, modifiable, or
 * deletable if it belongs to the given owner.
 * </p>
 *
 * <p>
 * Typical use cases include relationships such as:
 * <ul>
 *   <li>{@code /orders/{orderId}/items}</li>
 *   <li>{@code /departments/{deptId}/employees}</li>
 *   <li>{@code /projects/{projectId}/tasks}</li>
 * </ul>
 * </p>
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the unique identifier type of the sub-resource
 * @param <INPUT>    the input DTO type used to create or update
 * @param <OUTPUT>   the output DTO type returned to the consumer
 * @see OwnedReadProvider
 * @see OwnedWriteProvider
 */
public interface OwnedCrudProvider<OWNER_ID, ID, INPUT, OUTPUT> extends OwnedReadProvider<OWNER_ID, ID, OUTPUT>, OwnedWriteProvider<OWNER_ID, ID, INPUT, OUTPUT> {
}
