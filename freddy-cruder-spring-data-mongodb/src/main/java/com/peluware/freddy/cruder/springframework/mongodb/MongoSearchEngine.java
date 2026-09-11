package com.peluware.freddy.cruder.springframework.mongodb;

import com.peluware.freddy.cruder.mongodb.SearchFilterBuilder;
import com.peluware.freddy.cruder.springframework.SearchEngine;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

/**
 * {@link SearchEngine} backed by Spring Data MongoDB's {@link MongoOperations}.
 *
 * <p>Converts the {@link Bson} filter produced by {@link SearchFilterBuilder} to a Spring
 * Data {@link Query} via relaxed extended JSON serialization, then delegates pagination and
 * count to {@link PageableExecutionUtils} to avoid unnecessary count queries on the last page.</p>
 */
public class MongoSearchEngine implements SearchEngine {

    private final MongoOperations mongoOperations;
    private final SearchFilterBuilder searchFilterBuilder;

    public MongoSearchEngine(MongoOperations mongoOperations, SearchFilterBuilder searchFilterBuilder) {
        this.mongoOperations = mongoOperations;
        this.searchFilterBuilder = searchFilterBuilder;
    }

    @Override
    public <T> Page<T> findAllBySearch(Class<T> entityType, @Nullable String search, @Nullable String query, Pageable pageable) {
        var baseQuery = getFilterQuery(
            entityType,
            search,
            query
        );
        var pagedQuery = Query.of(baseQuery).with(pageable);
        var content = mongoOperations.find(pagedQuery, entityType);
        return PageableExecutionUtils.getPage(
            content,
            pageable,
            () -> mongoOperations.count(baseQuery, entityType)
        );
    }


    @Override
    public <T> List<T> findAllBySearch(Class<T> entityType, @Nullable String search, @Nullable String query, Sort sort) {
        var baseQuery = getFilterQuery(entityType, search, query);
        if (sort.isSorted()) {
            baseQuery = baseQuery.with(sort);
        }
        return mongoOperations.find(baseQuery, entityType);
    }

    @Override
    public <T> long countBySearch(Class<T> entityType, @Nullable String search, @Nullable String query) {
        var baseQuery = getFilterQuery(
            entityType,
            search,
            query
        );
        return mongoOperations.count(baseQuery, entityType);
    }


    private <T> Query getFilterQuery(Class<T> entityType, @Nullable String search, @Nullable String query) {
        var filter = searchFilterBuilder.build(entityType, search, query);
        var bsonDoc = filter.toBsonDocument(BsonDocument.class, mongoOperations.getConverter().getCodecRegistry());
        return new BasicQuery(Document.parse(bsonDoc.toJson()));
    }

}
