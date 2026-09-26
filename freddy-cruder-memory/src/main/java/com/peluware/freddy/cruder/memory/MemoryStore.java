package com.peluware.freddy.cruder.memory;

import com.peluware.domain.Order;
import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Holds the entities of an in-memory provider by identifier in insertion order.
 */
final class MemoryStore<ENTITY, ID> {

    private final Map<ID, ENTITY> entities = new LinkedHashMap<>();
    private final MemoryIds<ENTITY, ID> ids;
    private boolean inTransaction;

    MemoryStore(MemoryIds<ENTITY, ID> ids) {
        this.ids = Objects.requireNonNull(ids, "Ids must not be null");
    }

    synchronized @Nullable ENTITY get(ID id) {
        var entity = entities.get(id);
        return entity == null ? null : copy(entity);
    }

    synchronized boolean contains(ID id) {
        return entities.containsKey(id);
    }

    synchronized ENTITY insert(ENTITY entity) {
        var id = ids.getter().apply(entity);
        if (id == null) {
            var generator = ids.generator();
            var setter = ids.setter();
            if (generator == null || setter == null) {
                throw new IllegalStateException("The entity has no identifier and none is generated");
            }
            id = generator.get();
            setter.accept(entity, id);
        }
        if (entities.containsKey(id)) {
            throw new IllegalStateException("Duplicate identifier [" + id + "]");
        }
        entities.put(id, copy(entity));
        return entity;
    }

    synchronized ENTITY replace(ENTITY entity) {
        var id = ids.getter().apply(entity);
        if (id == null || !entities.containsKey(id)) {
            throw new IllegalStateException("Cannot update an entity that is not stored");
        }
        entities.put(id, copy(entity));
        return entity;
    }

    synchronized void remove(ENTITY entity) {
        var id = ids.getter().apply(entity);
        if (id != null) {
            entities.remove(id);
        }
    }

    synchronized void clear() {
        entities.clear();
    }

    synchronized List<ENTITY> all() {
        var copies = new ArrayList<ENTITY>(entities.size());
        for (var entity : entities.values()) {
            copies.add(copy(entity));
        }
        return copies;
    }

    synchronized <T extends @Nullable Object> T transactional(Supplier<T> work) {
        if (inTransaction) {
            return work.get();
        }
        var snapshot = new LinkedHashMap<>(entities);
        inTransaction = true;
        try {
            return work.get();
        } catch (RuntimeException | Error e) {
            entities.clear();
            entities.putAll(snapshot);
            throw e;
        } finally {
            inTransaction = false;
        }
    }

    private ENTITY copy(ENTITY entity) {
        var copier = ids.copier();
        return copier == null ? entity : copier.apply(entity);
    }

    List<ENTITY> matching(Predicate<ENTITY> filter, Sort sort) {
        var result = new ArrayList<>(all().stream().filter(filter).toList());
        if (sort.isSorted()) {
            result.sort(comparator(sort));
        }
        return result;
    }

    Page<ENTITY> page(Predicate<ENTITY> filter, Pagination pagination, Sort sort) {
        var matching = matching(filter, sort);
        if (!pagination.isPaginated()) {
            return new Page<>(matching, pagination, sort, matching.size());
        }
        var from = (int) Math.min(pagination.getOffset(), matching.size());
        var to = (int) Math.min((long) from + pagination.getSize(), matching.size());
        return new Page<>(List.copyOf(matching.subList(from, to)), pagination, sort, matching.size());
    }

    private Comparator<ENTITY> comparator(Sort sort) {
        Comparator<ENTITY> result = null;
        for (var order : sort) {
            var next = comparator(order);
            result = result == null ? next : result.thenComparing(next);
        }
        return Objects.requireNonNull(result);
    }

    private Comparator<ENTITY> comparator(Order order) {
        var property = order.property();
        Comparator<@Nullable Object> values = Comparator.nullsLast((a, b) -> compare(property, a, b));
        Comparator<ENTITY> byProperty = (a, b) -> values.compare(read(a, property), read(b, property));
        return order.direction() == Order.Direction.ASC ? byProperty : byProperty.reversed();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int compare(String property, @Nullable Object a, @Nullable Object b) {
        if (!(a instanceof Comparable comparable)) {
            throw new IllegalArgumentException("Cannot sort by property '" + property + "': its values are not comparable");
        }
        try {
            return comparable.compareTo(Objects.requireNonNull(b));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot sort by property '" + property + "': its values are of different types", e);
        }
    }

    private static @Nullable Object read(Object target, String path) {
        Object current = target;
        for (var name : path.split("\\.")) {
            if (current == null) {
                return null;
            }
            current = readProperty(current, name);
        }
        return current;
    }

    private static @Nullable Object readProperty(Object target, String name) {
        var type = target.getClass();
        var capitalized = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        for (var accessor : new String[]{"get" + capitalized, "is" + capitalized, name}) {
            try {
                var method = type.getMethod(accessor);
                method.setAccessible(true);
                return method.invoke(target);
            } catch (NoSuchMethodException _) {
                // try the next accessor
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read property '" + name + "' of " + type.getName(), e);
            }
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException _) {
                // look in the superclass
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot read property '" + name + "' of " + type.getName(), e);
            }
        }
        throw new IllegalArgumentException("No property '" + name + "' in " + type.getName());
    }
}
