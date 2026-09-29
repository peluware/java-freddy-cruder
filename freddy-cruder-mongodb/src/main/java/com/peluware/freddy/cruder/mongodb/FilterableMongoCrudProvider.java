package com.peluware.freddy.cruder.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.NotFoundEntityException;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.conversions.Bson;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.stream.Stream;

/**
 * MongoDB-specific implementation of {@link EntityCrudProvider} that routes all read
 * queries through a {@link #filterFilter} hook before execution.
 *
 * <p>Use this provider when you need to inject a global filter into every read operation
 * (soft-delete, multi-tenancy, row-level access control). The hook receives the
 * operation filter and returns the final filter to apply.</p>
 *
 * @param <ENTITY> the MongoDB document type
 * @param <ID>     the document identifier type
 * @param <INPUT>  the input DTO type for create/update operations
 * @param <OUTPUT> the output DTO or projection type
 */
public abstract class FilterableMongoCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final MongoDatabase mongoDatabase;
    protected final SearchFilterBuilder searchFilterBuilder;

    // ------------------------------------------------------------
    // CONSTRUCTORS — explicit entityClass
    // ------------------------------------------------------------

    public FilterableMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.mongoDatabase = mongoDatabase;
        this.searchFilterBuilder = searchFilterBuilder;
    }

    public FilterableMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, Class<ENTITY> entityClass) {
        this(mongoDatabase, searchFilterBuilder, entityClass, EntityCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // CONSTRUCTORS — reflection-based entityClass
    // ------------------------------------------------------------

    public FilterableMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.mongoDatabase = mongoDatabase;
        this.searchFilterBuilder = searchFilterBuilder;
    }

    public FilterableMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder) {
        this(mongoDatabase, searchFilterBuilder, EntityCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // COLLECTION
    // ------------------------------------------------------------

    /**
     * Returns the MongoDB collection name for this provider.
     * Defaults to the entity class simple name lowercased. Override to customize.
     */
    protected String collectionName() {
        return entityClass.getSimpleName().toLowerCase();
    }

    /**
     * Returns the typed MongoDB collection for this provider's entity type.
     */
    protected MongoCollection<ENTITY> collection() {
        return mongoDatabase.getCollection(collectionName(), entityClass);
    }

    // ------------------------------------------------------------
    // QUERY INFRASTRUCTURE
    // ------------------------------------------------------------

    /**
     * Executes a find against the collection, routing {@code filter} through {@link #filterFilter} first.
     */
    protected final List<ENTITY> runFind(Bson filter, Pagination pagination, Sort sort) {
        return MongoQueryHelpers.find(collection(), filterFilter(filter), pagination, sort);
    }

    /**
     * Executes a find against the collection, routing {@code filter} through {@link #filterFilter} first.
     */
    protected final List<ENTITY> runList(Bson filter, Sort sort) {
        return MongoQueryHelpers.find(collection(), filterFilter(filter), sort);
    }

    /**
     * Streams a find against the collection, routing {@code filter} through {@link #filterFilter} first.
     */
    protected final Stream<ENTITY> runStream(Bson filter, Sort sort) {
        return MongoQueryHelpers.stream(collection(), filterFilter(filter), sort);
    }

    /**
     * Counts documents matching {@code filter}, routing it through {@link #filterFilter} first.
     */
    protected final long runCount(Bson filter) {
        return MongoQueryHelpers.count(collection(), filterFilter(filter));
    }

    /**
     * Checks existence against {@code filter}, routing it through {@link #filterFilter} first.
     */
    protected final boolean runExists(Bson filter) {
        return MongoQueryHelpers.exists(collection(), filterFilter(filter));
    }

    /**
     * Applies a global filter to every read query. The default returns the original filter unchanged.
     * Override to inject cross-cutting conditions such as:
     *
     * <ul>
     *   <li>Soft-delete: {@code Filters.and(original, Filters.ne("deleted", true))}</li>
     *   <li>Multi-tenant: {@code Filters.and(original, Filters.eq("tenantId", currentTenant()))}</li>
     * </ul>
     */
    protected Bson filterFilter(Bson original) {
        return original;
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        var entity = collection()
            .find(filterFilter(Filters.eq("_id", id)))
            .first();
        if (entity == null) throw new NotFoundEntityException(entityClass, id);
        return entity;
    }

    @Override
    protected Page<ENTITY> internalPage(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = runFind(buildSearchFilter(search, query), pagination, sort);
        return Page.deferred(content, pagination, sort, () -> internalCount(search, query));
    }

    @Override
    protected List<ENTITY> internalList(@Nullable String search, @Nullable String query, Sort sort) {
        return runList(buildSearchFilter(search, query), sort);
    }

    @Override
    protected Stream<ENTITY> internalStream(@Nullable String search, @Nullable String query, Sort sort) {
        return runStream(buildSearchFilter(search, query), sort);
    }

    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return runCount(buildSearchFilter(search, query));
    }

    @Override
    protected boolean internalExists(ID id) {
        return runExists(Filters.eq("_id", id));
    }

    @Override
    protected ENTITY internalCreate(ENTITY entity) {
        collection().insertOne(entity);
        return entity;
    }

    @Override
    protected ENTITY internalUpdate(ENTITY entity) {
        collection().replaceOne(Filters.eq("_id", extractId(entity)), entity);
        return entity;
    }

    @Override
    protected void internalDelete(ENTITY entity) {
        collection().deleteOne(Filters.eq("_id", extractId(entity)));
    }

    // ------------------------------------------------------------
    // HOOKS
    // ------------------------------------------------------------

    /**
     * Builds the search filter from the given search and query strings.
     * Delegates to {@link SearchFilterBuilder} by default. Override to customize.
     */
    protected Bson buildSearchFilter(@Nullable String search, @Nullable String query) {
        return searchFilterBuilder.build(entityClass, search, query);
    }

    /**
     * Extracts the identifier from a document instance. Used for update and delete operations.
     *
     * <p>The default implementation reads the field annotated with {@link BsonId}
     * or the first field named {@code id} or {@code _id}. Override if a different strategy is needed.</p>
     */
    protected ID extractId(ENTITY entity) {
        return MongoUtils.extractId(entityClass, entity);
    }
}
