package com.peluware.freddy.cruder;

/**
 * Composes all write operations for a sub-resource scoped to an owner:
 * creation, update, and deletion.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the unique identifier type of the sub-resource
 * @param <INPUT>    the input DTO type used to create or update
 * @param <OUTPUT>   the output DTO type returned to the consumer
 * @see OwnedCreateProvider
 * @see OwnedUpdateProvider
 * @see OwnedDeleteProvider
 */
public interface OwnedWriteProvider<OWNER_ID, ID, INPUT, OUTPUT> extends
        OwnedCreateProvider<OWNER_ID, INPUT, OUTPUT>,
        OwnedUpdateProvider<OWNER_ID, ID, INPUT, OUTPUT>,
        OwnedDeleteProvider<OWNER_ID, ID> {
}
