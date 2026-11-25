package com.peluware.freddy.cruder.mongodb;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.peluware.domain.Order;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.NotFoundEntityException;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MongoCrudProviderTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static MongoClient client;
    static MongoDatabase database;

    PersonProvider people;

    public static class Person {
        public Long id;
        public String name;
        public int age;

        public Person() {
        }
    }

    record PersonInput(long id, String name, int age) {
    }

    record PersonOutput(Long id, String name, int age) {
    }

    /** Un buscador que reconoce solo {@code search} como prefijo del nombre; el resto no filtra. */
    static final SearchFilterBuilder NAME_PREFIX = new SearchFilterBuilder() {
        @Override
        public <E> Bson build(Class<E> documentClass, @Nullable String search, @Nullable String query) {
            return search == null ? new org.bson.Document() : Filters.regex("name", "^" + search);
        }
    };

    static class PersonProvider extends MongoCrudProvider<Person, Long, PersonInput, PersonOutput> {

        PersonProvider() {
            super(database, NAME_PREFIX, Person.class);
        }

        @Override
        protected void mapInput(PersonInput input, Person entity, boolean isNew) {
            if (isNew) {
                entity.id = input.id();
            }
            entity.name = input.name();
            entity.age = input.age();
        }

        @Override
        protected PersonOutput mapOutput(Person entity) {
            return new PersonOutput(entity.id, entity.name, entity.age);
        }
    }

    @BeforeAll
    static void connect() {
        var registry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        client = MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(new com.mongodb.ConnectionString(MONGO.getConnectionString()))
            .codecRegistry(registry)
            .build());
        database = client.getDatabase("freddy");
    }

    @AfterAll
    static void disconnect() {
        if (client != null) {
            client.close();
        }
    }

    @BeforeEach
    void freshCollection() {
        database.getCollection("person").drop();
        people = new PersonProvider();
    }

    @Test
    @DisplayName("crea un documento y lo vuelve a leer por su identificador")
    void createAndFind() {
        var created = people.create(new PersonInput(1, "Ana", 30));

        assertEquals(new PersonOutput(1L, "Ana", 30), created);
        assertEquals(created, people.find(1L));
        assertTrue(people.exists(1L));
        assertFalse(people.exists(2L));
    }

    @Test
    @DisplayName("buscar uno que no existe lanza NotFoundEntityException")
    void findMissingThrows() {
        assertThrows(NotFoundEntityException.class, () -> people.find(99L));
    }

    @Test
    @DisplayName("actualiza reemplazando el documento y conserva el identificador")
    void update() {
        people.create(new PersonInput(1, "Ana", 30));

        var updated = people.update(1L, new PersonInput(1, "Ana María", 31));

        assertEquals(new PersonOutput(1L, "Ana María", 31), updated);
        assertEquals(updated, people.find(1L));
        assertEquals(1, people.count(null, null));
    }

    @Test
    @DisplayName("elimina el documento")
    void delete() {
        people.create(new PersonInput(1, "Ana", 30));

        people.delete(1L);

        assertFalse(people.exists(1L));
        assertThrows(NotFoundEntityException.class, () -> people.delete(1L));
    }

    @Test
    @DisplayName("list respeta el orden pedido y el filtro de búsqueda")
    void listSortsAndFilters() {
        people.create(new PersonInput(1, "Ana", 30));
        people.create(new PersonInput(2, "Alberto", 50));
        people.create(new PersonInput(3, "Berta", 40));

        var oldestFirst = people.list(null, null, Sort.by("age", Order.Direction.DESC));
        var startingWithA = people.list("A", null, Sort.by("age", Order.Direction.ASC));

        assertIterableEquals(List.of("Alberto", "Berta", "Ana"), oldestFirst.stream().map(PersonOutput::name).toList());
        assertIterableEquals(List.of("Ana", "Alberto"), startingWithA.stream().map(PersonOutput::name).toList());
        assertEquals(2, people.count("A", null));
    }

    @Test
    @DisplayName("page devuelve la ventana pedida y el total de todo lo que coincide")
    void pageWindowAndTotal() {
        for (var i = 1; i <= 5; i++) {
            people.create(new PersonInput(i, "P" + i, i));
        }

        var page = people.page(null, null, Pagination.of(1, 2), Sort.by("age", Order.Direction.ASC));

        assertIterableEquals(List.of("P3", "P4"), page.getContent().stream().map(PersonOutput::name).toList());
        assertEquals(5, page.getTotalElements());
    }

    @Test
    @DisplayName("stream recorre el cursor en orden y al cerrarlo libera el cursor")
    void streamIsOrderedAndClosable() {
        people.create(new PersonInput(1, "Ana", 30));
        people.create(new PersonInput(2, "Berta", 20));

        try (var stream = people.stream(null, null, Sort.by("age", Order.Direction.ASC))) {
            assertIterableEquals(List.of("Berta", "Ana"), stream.map(PersonOutput::name).toList());
        }
    }
}
