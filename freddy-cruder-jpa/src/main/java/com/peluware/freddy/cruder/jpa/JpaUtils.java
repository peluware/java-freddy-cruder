package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Sort;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.CollectionAttribute;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.MapAttribute;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class JpaUtils {

    private JpaUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Map<Class<?>, String> ID_FIELD_CACHE = new ConcurrentHashMap<>();

    /**
     * Resolves the name of the entity's single identifier attribute, caching the result per class.
     *
     * @param metamodel   the metamodel used to resolve the identifier
     * @param entityClass the entity class
     * @return the identifier attribute name
     * @throws IllegalArgumentException if the entity is mapped with {@code @IdClass} (multiple {@code @Id} fields)
     */
    public static String getIdFieldName(Metamodel metamodel, Class<?> entityClass) {
        return ID_FIELD_CACHE.computeIfAbsent(entityClass, cls -> {
            var et = metamodel.entity(cls);
            return et.getId(et.getIdType().getJavaType()).getName();
        });
    }

    public static List<Order> getOrders(Sort sort, Path<?> root, CriteriaBuilder cb, Metamodel metamodel) {
        return sort.orders().stream()
            .map(order -> {
                var path = findPath(order.property(), root, metamodel, JoinType.LEFT);
                return order.direction() == com.peluware.domain.Order.Direction.ASC
                    ? cb.asc(path)
                    : cb.desc(path);
            })
            .toList();
    }

    /**
     * Find a property path in the graph From startRoot, using INNER JOIN for associations and element collections.
     *
     * @param path       The property path to find.
     * @param startRoot  From that property path depends on.
     * @param metamodel  the metamodel used to resolve attribute paths.
     * @return The Path for the property path
     * @throws IllegalArgumentException if attribute of the given property name does not exist
     */
    public static Path<?> findPath(String path, Path<?> startRoot, Metamodel metamodel) {
        return findPath(path, startRoot, metamodel, JoinType.INNER);
    }


    /**
     * Find a property path in the graph From startRoot
     *
     * @param path       The property path to find.
     * @param startRoot  From that property path depends on.
     * @param metamodel  the metamodel used to resolve attribute paths.
     * @param joinType   The type of join to use for associations and element collections.
     * @return The Path for the property path
     * @throws IllegalArgumentException if attribute of the given property name does not exist
     */
    public static Path<?> findPath(String path, Path<?> startRoot, Metamodel metamodel, JoinType joinType) {
        var graph = path.split("\\.");

        var classMetadata = metamodel.managedType(startRoot.getJavaType());
        var currentRoot = startRoot;

        var graphLength = graph.length;
        for (int i = 0; i < graphLength; i++) {
            var attribute = graph[i];

            if (!hasAttribute(attribute, classMetadata)) {
                throw new IllegalArgumentException("Unknown property: " + attribute + " in entity " + classMetadata.getJavaType().getName());
            }

            var jpaAttribute = classMetadata.getAttribute(attribute);
            var persistentAttributeType = jpaAttribute.getPersistentAttributeType();
            var attributeType = getSingularType(jpaAttribute);

            if (jpaAttribute.isAssociation()) {

                if (!jpaAttribute.isCollection()
                    && i == graphLength - 2
                    && isSingleIdNamed(metamodel, attributeType, graph[i + 1])) {
                    return currentRoot.get(attribute).get(graph[i + 1]);
                }

                classMetadata = metamodel.managedType(attributeType);
                currentRoot = getOrCreateJoin((From<?, ?>) currentRoot, attribute, joinType);

            } else if (persistentAttributeType == Attribute.PersistentAttributeType.EMBEDDED) {

                classMetadata = metamodel.embeddable(attributeType);
                currentRoot = currentRoot.get(attribute);

            } else if (persistentAttributeType == Attribute.PersistentAttributeType.ELEMENT_COLLECTION) {

                currentRoot = getOrCreateJoin((From<?, ?>) currentRoot, attribute, joinType);
                if (i != graphLength - 1) {
                    throw new IllegalArgumentException("ElementCollection must be the last part of the path: " + path);
                }

            } else if (persistentAttributeType == Attribute.PersistentAttributeType.BASIC) {

                currentRoot = currentRoot.get(attribute);
                if (i != graphLength - 1) {
                    throw new IllegalArgumentException("Basic attribute must be the last part of the path: " + path);
                }

            }
        }

        return currentRoot;
    }


    /**
     * Whether {@code attribute} is the single identifier of {@code entityClass}. Entities mapped
     * with {@code @IdClass} answer {@code false}: they have no single id attribute to name.
     */
    private static boolean isSingleIdNamed(Metamodel metamodel, Class<?> entityClass, String attribute) {
        var entityType = metamodel.entity(entityClass);
        return entityType.hasSingleIdAttribute()
            && entityType.getId(entityType.getIdType().getJavaType()).getName().equals(attribute);
    }

    /**
     * Verifies if a class metamodel has the specified property.
     *
     * @param property      Property name.
     * @param classMetadata Class metamodel that may hold that property.
     * @return {@code true} if the class has that property, {@code false} otherwise.
     */
    public static <E> boolean hasAttribute(String property, ManagedType<E> classMetadata) {
        Set<Attribute<? super E, ?>> names = classMetadata.getAttributes();
        for (Attribute<? super E, ?> name : names) {
            if (name.getName().equals(property)) return true;
        }
        return false;
    }

    /**
     * Get the property Type out of the metamodel.
     *
     * @param property Property name for type extraction.
     * @return Class java type for the property,
     * if the property is a pluralAttribute it will take the bindable java type of that collection.
     */
    public static Class<?> getSingularType(Attribute<?, ?> property) {
        if (property.isCollection()) {
            return ((PluralAttribute<?, ?, ?>) property).getBindableJavaType();
        }
        return property.getJavaType();
    }

    private static @Nullable Join<?, ?> findJoin(From<?, ?> from, String attribute, JoinType joinType) {
        for (var join : from.getJoins()) {
            if (join.getAttribute().getName().equals(attribute) && join.getJoinType() == joinType) {
                return join;
            }
        }
        return null;
    }

    /**
     * Returns the join of {@code from} on the attribute named {@code attribute} with {@code joinType},
     * creating it when there is none yet.
     *
     * @param from      the source to join from
     * @param attribute the name of the attribute to join through
     * @param joinType  the type of join
     * @return the existing join, or a new one
     */
    public static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute, JoinType joinType) {
        var existing = findJoin(from, attribute, joinType);
        return existing != null ? existing : from.join(attribute, joinType);
    }

    /**
     * Same as {@link #getOrCreateJoin(From, String, JoinType)}, but for a {@code List} attribute from
     * the metamodel, returning a {@link ListJoin} typed with it.
     */
    @SuppressWarnings("unchecked")
    public static <X, Y> ListJoin<X, Y> getOrCreateJoin(From<?, X> from, ListAttribute<? super X, Y> attribute, JoinType joinType) {
        var existing = findJoin(from, attribute.getName(), joinType);
        return existing != null ? (ListJoin<X, Y>) existing : from.join(attribute, joinType);
    }

    /**
     * Same as {@link #getOrCreateJoin(From, String, JoinType)}, but for a {@code Set} attribute from
     * the metamodel, returning a {@link SetJoin} typed with it.
     */
    @SuppressWarnings("unchecked")
    public static <X, Y> SetJoin<X, Y> getOrCreateJoin(From<?, X> from, SetAttribute<? super X, Y> attribute, JoinType joinType) {
        var existing = findJoin(from, attribute.getName(), joinType);
        return existing != null ? (SetJoin<X, Y>) existing : from.join(attribute, joinType);
    }

    /**
     * Same as {@link #getOrCreateJoin(From, String, JoinType)}, but for a {@code Collection} attribute
     * from the metamodel, returning a {@link CollectionJoin} typed with it.
     */
    @SuppressWarnings("unchecked")
    public static <X, Y> CollectionJoin<X, Y> getOrCreateJoin(From<?, X> from, CollectionAttribute<? super X, Y> attribute, JoinType joinType) {
        var existing = findJoin(from, attribute.getName(), joinType);
        return existing != null ? (CollectionJoin<X, Y>) existing : from.join(attribute, joinType);
    }

    /**
     * Same as {@link #getOrCreateJoin(From, String, JoinType)}, but for a {@code Map} attribute from
     * the metamodel, returning a {@link MapJoin} typed with its key and value.
     */
    @SuppressWarnings("unchecked")
    public static <X, K, V> MapJoin<X, K, V> getOrCreateJoin(From<?, X> from, MapAttribute<? super X, K, V> attribute, JoinType joinType) {
        var existing = findJoin(from, attribute.getName(), joinType);
        return existing != null ? (MapJoin<X, K, V>) existing : from.join(attribute, joinType);
    }

    /**
     * Same as {@link #getOrCreateJoin(From, String, JoinType)}, but takes the attribute from the
     * metamodel and returns a {@link Join} typed with it. Reuses an existing join on the same
     * attribute and join type.
     *
     * @param from      the source to join from
     * @param attribute the metamodel attribute to join through
     * @param joinType  the type of join
     * @return the existing join on {@code attribute} with {@code joinType}, or a new one
     */
    @SuppressWarnings("unchecked")
    public static <X, Y> Join<X, Y> getOrCreateJoin(From<?, X> from, SingularAttribute<? super X, Y> attribute, JoinType joinType) {
        var existing = findJoin(from, attribute.getName(), joinType);
        return existing != null ? (Join<X, Y>) existing : from.join(attribute, joinType);
    }

}
