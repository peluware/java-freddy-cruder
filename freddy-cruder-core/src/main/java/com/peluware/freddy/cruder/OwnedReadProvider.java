package com.peluware.freddy.cruder;

/**
 * Composes all read operations for a sub-resource scoped to an owner:
 * pagination, lookup, count, and existence check.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the unique identifier type of the sub-resource
 * @param <OUTPUT>   the output DTO type returned to the consumer
 * @see OwnedPageProvider
 * @see OwnedFindProvider
 * @see OwnedCountProvider
 * @see OwnedExistsProvider
 */
public interface OwnedReadProvider<OWNER_ID, ID, OUTPUT> extends
        OwnedPageProvider<OWNER_ID, OUTPUT>,
        OwnedFindProvider<OWNER_ID, ID, OUTPUT>,
        OwnedCountProvider<OWNER_ID>,
        OwnedExistsProvider<OWNER_ID, ID> {
}
