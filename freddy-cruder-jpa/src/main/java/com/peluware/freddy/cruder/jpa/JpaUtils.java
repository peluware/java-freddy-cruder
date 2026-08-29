package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Sort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.PluralAttribute;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
                throw new IllegalArgumentException("Unknown property: " + attribute + " From<?,?> entity " + classMetadata.getJavaType().getName());
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
     * @return <tt>true</tt> if the class has that property, <tt>false</tt> otherwise.
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


    public static Join<?, ?> getOrCreateJoin(From<?, ?> from, String attribute, JoinType joinType) {
        return from.getJoins().stream()
            .filter(join -> join.getAttribute().getName().equals(attribute) && join.getJoinType() == joinType)
            .findFirst()
            .orElseGet(() -> from.join(attribute, joinType));
    }

    /**
     * Executes {@code function} within a transaction, honoring any existing external
     * transaction (Spring, JTA, or container-managed).
     *
     * <p>
     * If the entity manager is already joined to an active transaction
     * ({@link EntityManager#isJoinedToTransaction()} returns {@code true}),
     * the function executes directly without opening a new transaction —
     * the caller's transaction boundary is reused.
     * </p>
     *
     * <p>
     * If no transaction is active, the method attempts to start and manage a
     * resource-local {@link EntityTransaction}. If the entity manager is
     * JTA-managed (e.g. in a Jakarta EE container or Spring with JTA), calling
     * {@link EntityManager#getTransaction()} is not permitted and will throw
     * {@link IllegalStateException} — in that case the function executes directly,
     * delegating transaction management to the JTA coordinator.
     * </p>
     *
     * @param em       the entity manager whose transaction context is checked
     * @param function the operation to execute
     * @param <T>      the return type
     * @return the result of the function
     */
    public static <T> T requireTransaction(EntityManager em, Supplier<T> function) {
        if (em.isJoinedToTransaction()) {
            return function.get();
        }
        try {
            return requireTransaction(em.getTransaction(), function);
        } catch (IllegalStateException e) {
            // JTA-managed EntityManager — transaction boundary is the container's responsibility
            return function.get();
        }
    }

    public static <T> T requireTransaction(EntityTransaction transaction, Supplier<T> function) {
        boolean weStartedIt = !transaction.isActive();

        try {
            if (weStartedIt) {
                transaction.begin();
            }

            T result = function.get();

            if (weStartedIt) {
                transaction.commit();
            }

            return result;
        } catch (RuntimeException e) {
            if (weStartedIt && transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
}
