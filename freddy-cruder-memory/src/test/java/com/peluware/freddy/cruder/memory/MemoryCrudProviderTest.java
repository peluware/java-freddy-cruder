package com.peluware.freddy.cruder.memory;

import com.peluware.domain.Order;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.NotFoundEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryCrudProviderTest {

    private PersonProvider people;

    @BeforeEach
    void setUp() {
        people = new PersonProvider();
    }

    @Test
    void createsAnEntityWithAGeneratedIdAndReadsItBack() {
        var created = people.create(new PersonInput("Ana", 30));

        assertEquals(1L, created.id());
        assertEquals(new PersonOutput(1L, "Ana", 30), people.find(1L));
    }

    @Test
    void updatesAnEntity() {
        var id = people.create(new PersonInput("Ana", 30)).id();

        people.update(id, new PersonInput("Ana María", 31));

        assertEquals(new PersonOutput(id, "Ana María", 31), people.find(id));
    }

    @Test
    void deletesAnEntityAndFailsToFindItAfterwards() {
        var id = people.create(new PersonInput("Ana", 30)).id();

        people.delete(id);

        assertFalse(people.exists(id));
        assertThrows(NotFoundEntityException.class, () -> people.find(id));
        assertThrows(NotFoundEntityException.class, () -> people.delete(id));
    }

    @Test
    void listsCountsAndSortsEntities() {
        people.create(new PersonInput("Ana", 30));
        people.create(new PersonInput("Luis", 25));
        people.create(new PersonInput("María", 41));

        var oldestFirst = people.list(null, null, Sort.by("age", Order.Direction.DESC));

        assertEquals(3L, people.count(null, null));
        assertEquals(List.of("María", "Ana", "Luis"), oldestFirst.stream().map(PersonOutput::name).toList());
    }

    @Test
    void rollsBackATransactionThatFails() {
        people.create(new PersonInput("Ana", 30));

        assertThrows(IllegalStateException.class, () -> people.inTransaction(() -> {
            people.create(new PersonInput("Luis", 25));
            throw new IllegalStateException("boom");
        }));

        assertEquals(1, people.entities().size());
        assertTrue(people.exists(1L));
        assertFalse(people.exists(2L));
    }
}
