package com.peluware.freddy.cruder.mongodb;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudEvents;
import com.peluware.freddy.cruder.NotFoundEntityException;
import com.peluware.freddy.cruder.OwnedEntityCrudProvider;
import com.peluware.freddy.cruder.OwnedId;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.conversions.Bson;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * MongoDB-specific implementation of {@link OwnedEntityCrudProvider} that scopes
 * all queries to an owner and routes them through a {@link #filterFilter} hook.
 *
 * <p>Every read operation combines {@link #buildOwnerFilter} with the operation filter
 * before passing through {@link #filterFilter}. Subclasses must implement
 * {@link #buildOwnerFilter} to enforce the ownership constraint.</p>
 *
 * @param <ENTITY>   the MongoDB document type
 * @param <OWNER_ID> the owner identifier type
 * @param <ID>       the document identifier type
 * @param <INPUT>    the input DTO type for create/update operations
 * @param <OUTPUT>   the output DTO or projection type
 */
public abstract class FilterableOwnedMongoCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> extends OwnedEntityCrudProvider<ENTITY, OWNER_ID, ID, INPUT, OUTPUT> {

    protected final MongoDatabase mongoDatabase;
    protected final SearchFilterBuilder searchFilterBuilder;

    // ------------------------------------------------------------
    // CONSTRUCTORS — explicit entityClass
    // ------------------------------------------------------------

    public FilterableOwnedMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.mongoDatabase = mongoDatabase;
        this.searchFilterBuilder = searchFilterBuilder;
    }

    public FilterableOwnedMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, Class<ENTITY> entityClass) {
        this(mongoDatabase, searchFilterBuilder, entityClass, EntityCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // CONSTRUCTORS — reflection-based entityClass
    // ------------------------------------------------------------

    public FilterableOwnedMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.mongoDatabase = mongoDatabase;
        this.searchFilterBuilder = searchFilterBuilder;
    }

    public FilterableOwnedMongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder) {
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
     *   <li>Row-level security constraints&lt;/li&gt;
     * </ul>
     *
     * <p>The {@code original} filter already includes the owner constraint from {@link #buildOwnerFilter}.</p>
     */
    protected Bson filterFilter(Bson original) {
        return original;
    }

    /**
     * Builds a filter that restricts queries to documents belonging to the given owner.
     * Called for every read operation. Common implementations:
     *
     * <ul>
     *   <li>Foreign key: {@code Filters.eq("userId", ownerId)}</li>
     *   <li>Tenant column: {@code Filters.eq("tenantId", ownerId)}</li>
     * </ul>
     */
    protected abstract Bson buildOwnerFilter(OWNER_ID ownerId);

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    @Override
    protected ENTITY internalFind(OWNER_ID ownerId, ID id) throws NotFoundEntityException {
        var entity = collection()
            .find(filterFilter(Filters.and(buildOwnerFilter(ownerId), Filters.eq("_id", id))))
            .first();
        if (entity == null) throw new NotFoundEntityException(entityClass, new OwnedId<>(ownerId, id));
        return entity;
    }

    @Override
    protected Page<ENTITY> internalPage(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var filter = Filters.and(buildOwnerFilter(ownerId), buildSearchFilter(search, query));
        var content = runFind(filter, pagination, sort);
        return Page.deferred(content, pagination, sort, () -> internalCount(ownerId, search, query));
    }

    @Override
    protected List<ENTITY> internalList(OWNER_ID ownerId, @Nullable String search, @Nullable String query, Sort sort) {
        return runList(Filters.and(buildOwnerFilter(ownerId), buildSearchFilter(search, query)), sort);
    }

    @Override
    protected long internalCount(OWNER_ID ownerId, @Nullable String search, @Nullable String query) {
        return runCount(Filters.and(buildOwnerFilter(ownerId), buildSearchFilter(search, query)));
    }

    @Override
    protected boolean internalExists(OWNER_ID ownerId, ID id) {
        return runExists(Filters.and(buildOwnerFilter(ownerId), Filters.eq("_id", id)));
    }

    @Override
    protected ENTITY internalCreate(OWNER_ID ownerId, ENTITY entity) {
        collection().insertOne(entity);
        return entity;
    }

    @Override
    protected ENTITY internalUpdate(OWNER_ID ownerId, ENTITY entity) {
        collection().replaceOne(Filters.eq("_id", extractId(entity)), entity);
        return entity;
    }

    @Override
    protected void internalDelete(OWNER_ID ownerId, ENTITY entity) {
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
