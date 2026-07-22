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

/**
 * MongoDB-specific implementation of {@link EntityCrudProvider} using the MongoDB sync driver.
 *
 * <p>Delegates search and count filtering to a {@link SearchFilterBuilder}.
 * For read queries with a global filter (soft-delete, multi-tenancy), prefer
 * {@link FilterableMongoCrudProvider}, which adds a {@code filterFilter} hook.</p>
 *
 * <p>The collection is resolved via {@link #collectionName()}, which defaults to the
 * entity class simple name lowercased. The {@code MongoDatabase} must be configured
 * with a codec registry that supports the entity type (e.g. via {@code PojoCodecProvider}).</p>
 *
 * @param <ENTITY> the MongoDB document type
 * @param <ID>     the document identifier type
 * @param <INPUT>  the input DTO type for create/update operations
 * @param <OUTPUT> the output DTO or projection type
 */
public abstract class MongoCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final MongoDatabase mongoDatabase;
    protected final SearchFilterBuilder searchFilterBuilder;

    // ------------------------------------------------------------
    // CONSTRUCTORS — explicit entityClass
    // ------------------------------------------------------------

    public MongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, Class<ENTITY> entityClass, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(entityClass, events);
        this.mongoDatabase = mongoDatabase;
        this.searchFilterBuilder = searchFilterBuilder;
    }

    public MongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, Class<ENTITY> entityClass) {
        this(mongoDatabase, searchFilterBuilder, entityClass, EntityCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // CONSTRUCTORS — reflection-based entityClass
    // ------------------------------------------------------------

    public MongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder, EntityCrudEvents<ENTITY, ID, INPUT> events) {
        super(events);
        this.mongoDatabase = mongoDatabase;
        this.searchFilterBuilder = searchFilterBuilder;
    }

    public MongoCrudProvider(MongoDatabase mongoDatabase, SearchFilterBuilder searchFilterBuilder) {
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
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        var entity = collection().find(Filters.eq("_id", id)).first();
        if (entity == null) throw new NotFoundEntityException(entityClass, id);
        return entity;
    }

    @Override
    protected Page<ENTITY> internalPage(@Nullable String search, @Nullable String query, Pagination pagination, Sort sort) {
        var content = MongoQueryHelpers.find(collection(), buildSearchFilter(search, query), pagination, sort);
        return Page.deferred(content, pagination, sort, () -> internalCount(search, query));
    }

    @Override
    protected long internalCount(@Nullable String search, @Nullable String query) {
        return MongoQueryHelpers.count(collection(), buildSearchFilter(search, query));
    }

    @Override
    protected boolean internalExists(ID id) {
        return MongoQueryHelpers.exists(collection(), Filters.eq("_id", id));
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
