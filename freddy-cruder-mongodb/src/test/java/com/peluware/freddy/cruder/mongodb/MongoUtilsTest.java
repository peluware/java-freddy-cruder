package com.peluware.freddy.cruder.mongodb;

import org.bson.codecs.pojo.annotations.BsonId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MongoUtilsTest {

    static class Annotated {
        String id = "not-this-one";
        @BsonId
        String key = "annotated";
    }

    static class ByIdName {
        String id = "by-id";
    }

    static class ByUnderscoreName {
        String _id = "by-underscore";
    }

    static class Parent {
        Long id = 7L;
    }

    static class Child extends Parent {
        String name = "child";
    }

    static class NoId {
        String name = "nothing";
    }

    @Test
    @DisplayName("el campo con @BsonId gana sobre uno llamado id")
    void bsonIdWins() {
        assertEquals("annotated", MongoUtils.<Annotated, String>extractId(Annotated.class, new Annotated()));
    }

    @Test
    @DisplayName("sin anotación, usa el campo llamado id")
    void fallsBackToIdName() {
        assertEquals("by-id", MongoUtils.<ByIdName, String>extractId(ByIdName.class, new ByIdName()));
    }

    @Test
    @DisplayName("sin anotación ni id, usa el campo llamado _id")
    void fallsBackToUnderscoreIdName() {
        assertEquals("by-underscore", MongoUtils.<ByUnderscoreName, String>extractId(ByUnderscoreName.class, new ByUnderscoreName()));
    }

    @Test
    @DisplayName("busca el identificador en la jerarquía de clases")
    void searchesTheHierarchy() {
        assertEquals(7L, MongoUtils.<Child, Long>extractId(Child.class, new Child()));
    }

    @Test
    @DisplayName("sin ningún campo identificador, falla diciendo qué clase")
    void failsWithoutIdField() {
        var error = assertThrows(IllegalStateException.class, () -> MongoUtils.<NoId, Object>extractId(NoId.class, new NoId()));

        assertEquals("No @BsonId, 'id', or '_id' field found in NoId", error.getMessage());
    }
}
