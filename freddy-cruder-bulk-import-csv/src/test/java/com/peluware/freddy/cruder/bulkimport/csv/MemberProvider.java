package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.memory.MemoryIds;
import com.peluware.freddy.cruder.memory.OwnedMemoryCrudProvider;

/**
 * Creates the members of a team in memory.
 */
class MemberProvider extends OwnedMemoryCrudProvider<Member, Long, Long, MemberInput, Long> {

    MemberProvider() {
        super(MemoryIds.sequentialLong(Member::getId, Member::setId), Member.class);
    }

    @Override
    protected void mapInput(Long ownerId, MemberInput input, Member entity, boolean isNew) {
        entity.teamId = ownerId;
        entity.name = input.name;
    }

    @Override
    protected Long mapOutput(Long ownerId, Member entity) {
        return entity.id;
    }

    @Override
    protected boolean belongsTo(Long ownerId, Member entity) {
        return ownerId.equals(entity.teamId);
    }
}
