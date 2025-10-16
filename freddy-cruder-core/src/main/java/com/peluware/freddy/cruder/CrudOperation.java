package com.peluware.freddy.cruder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CrudOperation {
    /**
     * Represents a create operation (write only).
     * <br>
     * This operation involves creating a new entity.
     */
    CREATE(true, false),

    /**
     * Represents an update operation (write only).
     * <br>
     * This operation involves modifying an existing entity.
     */
    UPDATE(true, false),

    /**
     * Represents a delete operation (write only).
     * <br>
     * This operation involves deleting an entity.
     */
    DELETE(true, false),

    /**
     * Represents a paginated read operation.
     * <br>
     * This operation involves retrieving a paginated list of entities.
     */
    PAGE(false, true),

    /**
     * Represents a find operation (read only).
     * <br>
     * This operation involves retrieving a specific entity by its ID.
     */
    FIND(false, true),

    /**
     * Represents a count operation (read only).
     * <br>
     * This operation involves counting the total number of entities.
     */
    COUNT(false, true),

    /**
     * Represents an existence check operation (read only).
     * <br>
     * This operation checks if an entity exists by its ID.
     */
    EXISTS(false, true);

    /**
     * Indicates if the operation involves writing (creating, updating, or deleting).
     * <br>
     * A value of {@code true} means the operation is write-related.
     */
    private final boolean write;

    /**
     * Indicates if the operation involves reading (retrieving data or checking existence).
     * <br>
     * A value of {@code true} means the operation is read-related.
     */
    private final boolean read;

    /**
     * Checks if the operation is read-only.
     * <br>
     * An operation is considered read-only if it involves reading but not writing.
     *
     * @return {@code true} if the operation is read-only, {@code false} otherwise.
     */
    public boolean isReadOnly() {
        return read && !write;
    }

    /**
     * Checks if the operation is write-only.
     * <br>
     * An operation is considered write-only if it involves writing but not reading.
     *
     * @return {@code true} if the operation is write-only, {@code false} otherwise.
     */
    public boolean isWriteOnly() {
        return write && !read;
    }
}
