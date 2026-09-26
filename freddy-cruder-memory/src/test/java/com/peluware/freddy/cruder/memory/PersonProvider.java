package com.peluware.freddy.cruder.memory;

class PersonProvider extends MemoryCrudProvider<Person, Long, PersonInput, PersonOutput> {

    PersonProvider() {
        super(MemoryIds.sequentialLong(Person::getId, Person::setId), Person.class);
    }

    @Override
    protected void mapInput(PersonInput input, Person entity, boolean isNew) {
        entity.name = input.name();
        entity.age = input.age();
    }

    @Override
    protected PersonOutput mapOutput(Person entity) {
        return new PersonOutput(entity.id, entity.name, entity.age);
    }
}
