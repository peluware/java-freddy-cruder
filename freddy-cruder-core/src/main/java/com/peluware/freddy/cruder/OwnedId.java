package com.peluware.freddy.cruder;

/**
 * Represents the composite identifier of a sub-resource scoped to an owner.
 *
 * @param <OWNER_ID> the identifier type of the owning resource
 * @param <ID>       the identifier type of the sub-resource
 */
public record OwnedId<OWNER_ID, ID>(OWNER_ID ownerId, ID id) {

    @Override
    public String toString() {
        return ownerId + "/" + id;
    }
}
