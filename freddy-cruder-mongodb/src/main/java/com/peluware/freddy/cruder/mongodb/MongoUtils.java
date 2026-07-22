package com.peluware.freddy.cruder.mongodb;

import org.bson.codecs.pojo.annotations.BsonId;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility methods for MongoDB POJO document handling.
 */
public final class MongoUtils {

    private MongoUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<Class<?>, Field> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Extracts the identifier value from a document instance.
     *
     * <p>Resolves the ID field by looking for a {@link BsonId}-annotated field first,
     * then falling back to a field named {@code id} or {@code _id}. Searches the class
     * hierarchy. The resolved field is cached per document class.</p>
     *
     * @param documentClass the document class
     * @param document      the document instance
     * @param <E>           the document type
     * @param <ID>          the identifier type
     * @return the identifier value
     * @throws IllegalStateException if no ID field is found or cannot be accessed
     */
    @SuppressWarnings("unchecked")
    public static <E, ID> ID extractId(Class<E> documentClass, E document) {
        var field = ID_FIELD_CACHE.computeIfAbsent(documentClass, MongoUtils::resolveIdField);
        try {
            return (ID) field.get(document);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access ID field in " + documentClass.getSimpleName(), e);
        }
    }

    private static Field resolveIdField(Class<?> cls) {
        for (var field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(BsonId.class)) {
                field.setAccessible(true);
                return field;
            }
        }
        for (var field : cls.getDeclaredFields()) {
            if ("id".equals(field.getName()) || "_id".equals(field.getName())) {
                field.setAccessible(true);
                return field;
            }
        }
        var superclass = cls.getSuperclass();
        if (superclass != null && superclass != Object.class) {
            return resolveIdField(superclass);
        }
        throw new IllegalStateException(
            "No @BsonId, 'id', or '_id' field found in " + cls.getSimpleName()
        );
    }
}
