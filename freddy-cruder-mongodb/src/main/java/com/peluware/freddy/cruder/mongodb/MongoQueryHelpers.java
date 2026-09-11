package com.peluware.freddy.cruder.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Sorts;
import com.peluware.domain.Order;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

/**
 * Static helpers for common MongoDB collection operations.
 */
public final class MongoQueryHelpers {

    private MongoQueryHelpers() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Executes a find with optional sort, without pagination.
     */
    public static <T> List<T> find(MongoCollection<T> collection, Bson filter, Sort sort) {
        return find(collection, filter, Pagination.unpaginated(), sort);
    }

    /**
     * Executes a find with optional pagination and sort.
     */
    public static <T> List<T> find(MongoCollection<T> collection, Bson filter, Pagination pagination, Sort sort) {
        var iterable = collection.find(filter);
        if (sort.isSorted()) {
            iterable = iterable.sort(toSort(sort));
        }
        if (pagination.isPaginated()) {
            iterable = iterable
                .skip(pagination.getNumber() * pagination.getSize())
                .limit(pagination.getSize());
        }
        return iterable.into(new ArrayList<>());
    }

    /**
     * Counts documents matching the filter.
     */
    public static <T> long count(MongoCollection<T> collection, Bson filter) {
        return collection.countDocuments(filter);
    }

    /**
     * Returns {@code true} if at least one document matches the filter.
     * Uses a limit of 1 to short-circuit the scan.
     */
    public static <T> boolean exists(MongoCollection<T> collection, Bson filter) {
        return collection.countDocuments(filter, new CountOptions().limit(1)) > 0;
    }

    /**
     * Converts a {@link Sort} to a MongoDB sort {@link Bson}.
     */
    public static Bson toSort(Sort sort) {
        var orders = sort.orders().stream()
            .map(order -> order.direction() == Order.Direction.ASC
                ? Sorts.ascending(order.property())
                : Sorts.descending(order.property()))
            .toList();
        return orders.size() == 1 ? orders.getFirst() : Sorts.orderBy(orders);
    }
}
