package com.peluware.freddy.cruder.memory;

import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.ReadOnlyException;
import com.peluware.freddy.cruder.ReadOnlyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReadOnlyProviderTest {

    /** Solo lectura: sin input y sin mapInput. */
    static class PeopleQueries extends MemoryCrudProvider.ReadOnly<Person, Long, PersonOutput> {

        PeopleQueries() {
            super(MemoryIds.sequentialLong(Person::getId, Person::setId), Person.class);
        }

        @Override
        protected PersonOutput mapOutput(Person entity) {
            return new PersonOutput(entity.id, entity.name, entity.age);
        }
    }

    /** Solo lectura con dueño: una persona pertenece al "dueño" que coincide con su edad. */
    static class OwnedPeopleQueries extends OwnedMemoryCrudProvider.ReadOnly<Person, Integer, Long, PersonOutput> {

        OwnedPeopleQueries() {
            super(MemoryIds.sequentialLong(Person::getId, Person::setId), Person.class);
        }

        @Override
        protected boolean belongsTo(Integer ownerId, Person entity) {
            return entity.age == ownerId;
        }

        @Override
        protected PersonOutput mapOutput(Integer ownerId, Person entity) {
            return new PersonOutput(entity.id, entity.name, entity.age);
        }
    }

    @Test
    @DisplayName("con dueño: las lecturas se acotan al dueño y las escrituras lanzan ReadOnlyException")
    void ownedReadOnlyProvider() {
        var people = new OwnedPeopleQueries();
        var ana = new Person();
        ana.name = "Ana";
        ana.age = 30;
        var luis = new Person();
        luis.name = "Luis";
        luis.age = 40;
        people.seed(ana, luis);

        assertEquals(1, people.count(30, null, null));
        assertEquals("Luis", people.list(40, null, null, Sort.unsorted()).getFirst().name());

        assertThrows(ReadOnlyException.class, () -> people.create(30, null));
        assertThrows(ReadOnlyException.class, () -> people.update(30, 1L, null));
        assertThrows(ReadOnlyException.class, () -> people.delete(30, 1L));
        assertEquals(2, people.entities().size());
    }

    @Test
    @DisplayName("las lecturas de un provider de solo lectura funcionan")
    void readOnlyProviderReads() {
        var people = new PeopleQueries();
        var ana = new Person();
        ana.name = "Ana";
        ana.age = 30;
        people.seed(ana);

        assertEquals(1, people.count(null, null));
        assertEquals("Ana", people.list(null, null, Sort.unsorted()).getFirst().name());
        assertEquals(1, people.page(null, null, Pagination.unpaginated(), Sort.unsorted()).getContent().size());
    }

    @Test
    @DisplayName("create, update y delete lanzan ReadOnlyException, que es una UnsupportedOperationException")
    void writesAreRejected() {
        var people = new PeopleQueries();

        var onCreate = assertThrows(ReadOnlyException.class, () -> people.create(null));
        assertThrows(ReadOnlyException.class, () -> people.update(1L, null));
        assertThrows(ReadOnlyException.class, () -> people.delete(1L));

        assertInstanceOf(UnsupportedOperationException.class, onCreate);
        assertInstanceOf(ReadOnlyProvider.class, people);
        assertEquals(PeopleQueries.class, onCreate.getProviderClass());
        assertEquals(0, people.entities().size());
    }

    @Test
    @DisplayName("un provider de escritura normal se crea sin problema")
    void writingProviderIsUnaffected() {
        assertEquals("Ana", new PersonProvider().create(new PersonInput("Ana", 30)).name());
    }
}
