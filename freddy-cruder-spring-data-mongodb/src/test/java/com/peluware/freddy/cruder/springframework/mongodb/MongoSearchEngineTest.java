package com.peluware.freddy.cruder.springframework.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.peluware.freddy.cruder.mongodb.SearchFilterBuilder;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

@Testcontainers
class MongoSearchEngineTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static MongoClient client;
    static MongoTemplate template;

    @Document("people")
    static class Person {
        public String id;
        public String name;
        public int age;

        Person(String id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    /** Reconoce {@code search} como prefijo del nombre; sin búsqueda no filtra. */
    static final SearchFilterBuilder NAME_PREFIX = new SearchFilterBuilder() {
        @Override
        public <E> Bson build(Class<E> documentClass, String search, String query) {
            return search == null ? Filters.empty() : Filters.regex("name", "^" + search);
        }
    };

    final MongoSearchEngine engine = new MongoSearchEngine(template, NAME_PREFIX);

    @BeforeAll
    static void connect() {
        client = MongoClients.create(MONGO.getConnectionString());
        template = new MongoTemplate(client, "test");
    }

    @AfterAll
    static void disconnect() {
        client.close();
    }

    @BeforeEach
    void seed() {
        template.dropCollection(Person.class);
        template.insertAll(List.of(
            new Person("1", "Ana", 30),
            new Person("2", "Andrés", 25),
            new Person("3", "Beatriz", 41)
        ));
    }

    @Test
    @DisplayName("sin búsqueda devuelve todo y cuenta todo")
    void withoutSearchReturnsEverything() {
        assertEquals(3, engine.countBySearch(Person.class, null, null));
        assertEquals(3, engine.findAllBySearch(Person.class, null, null, Sort.unsorted()).size());
    }

    @Test
    @DisplayName("la búsqueda filtra los resultados y el conteo")
    void searchFiltersResultsAndCount() {
        assertEquals(2, engine.countBySearch(Person.class, "An", null));

        var names = engine.findAllBySearch(Person.class, "An", null, Sort.by("age")).stream()
            .map(person -> person.name)
            .toList();

        assertIterableEquals(List.of("Andrés", "Ana"), names);
    }

    @Test
    @DisplayName("una página trae el tamaño pedido y el total que coincide")
    void pageCarriesTheRequestedSliceAndTheTotal() {
        var page = engine.findAllBySearch(Person.class, "An", null, PageRequest.of(0, 1, Sort.by("age")));

        assertEquals(1, page.getContent().size());
        assertEquals("Andrés", page.getContent().getFirst().name);
        assertEquals(2, page.getTotalElements());
    }
}
