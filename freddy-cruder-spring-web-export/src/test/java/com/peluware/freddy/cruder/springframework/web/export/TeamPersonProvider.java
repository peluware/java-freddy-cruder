package com.peluware.freddy.cruder.springframework.web.export;

import com.peluware.freddy.cruder.memory.MemoryIds;
import com.peluware.freddy.cruder.memory.OwnedMemoryCrudProvider;

class TeamPersonProvider extends OwnedMemoryCrudProvider<TeamPerson, Long, Long, TeamPersonInput, TeamPersonOutput> {

    TeamPersonProvider() {
        super(MemoryIds.sequentialLong(TeamPerson::getId, TeamPerson::setId), TeamPerson.class);
    }

    @Override
    protected boolean belongsTo(Long ownerId, TeamPerson entity) {
        return ownerId.equals(entity.teamId);
    }

    @Override
    protected void mapInput(Long ownerId, TeamPersonInput input, TeamPerson entity, boolean isNew) {
        entity.teamId = ownerId;
        entity.name = input.name();
    }

    @Override
    protected TeamPersonOutput mapOutput(Long ownerId, TeamPerson entity) {
        return new TeamPersonOutput(entity.id, entity.name);
    }
}
