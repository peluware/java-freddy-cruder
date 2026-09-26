package com.peluware.freddy.cruder.bulkimport.csv;

import com.peluware.freddy.cruder.memory.MemoryCrudProvider;
import com.peluware.freddy.cruder.memory.MemoryIds;

/**
 * Creates people in memory. A person named {@code boom} cannot be created.
 */
class PersonProvider extends MemoryCrudProvider<Person, Long, PersonInput, Long> {

    PersonProvider() {
        super(MemoryIds.sequentialLong(Person::getId, Person::setId), Person.class);
    }

    @Override
    protected void mapInput(PersonInput input, Person entity, boolean isNew) {
        if (input.name.equals("boom")) {
            throw new IllegalStateException("Duplicate name");
        }
        entity.name = input.name;
        entity.age = input.age;
        entity.birthDate = input.birthDate;
        entity.role = input.role;
    }

    @Override
    protected Long mapOutput(Person entity) {
        return entity.id;
    }
}
