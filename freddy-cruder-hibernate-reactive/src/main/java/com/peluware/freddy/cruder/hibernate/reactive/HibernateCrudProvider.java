package com.peluware.freddy.cruder.hibernate.reactive;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.freddy.cruder.reactive.mutiny.MutinyCrudEvents;
import com.peluware.freddy.cruder.reactive.mutiny.MutinyEntityCrudProvider;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.hibernate.reactive.HibernateOmniSearch;
import cz.jirutka.rsql.parser.RSQLParser;
import io.smallrye.mutiny.Uni;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.function.Supplier;

/**
 * Hibernate Reactive-specific implementation of {@link MutinyEntityCrudProvider} that provides
 * standard CRUD operations for entities using a {@link Mutiny.SessionFactory}.
 *
 * <p>
 * This class integrates with {@link HibernateOmniSearch} to support search and pagination
 * via the OmniSearch API, and uses {@link RSQLParser} for parsing optional RSQL queries.
 * </p>
 *
 * <p>
 * It provides default implementations for finding, counting, creating, updating, and deleting
 * entities in a reactive, non-blocking manner, as well as checking existence. Subclasses
 * can override any behavior as needed.
 * </p>
 *
 * @param <ENTITY> the entity type
 * @param <ID>     the entity identifier type
 * @param <INPUT>  the input DTO type for create/update operations
 * @param <OUTPUT> the output DTO or projection type
 */
public abstract class HibernateCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends MutinyEntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final Mutiny.SessionFactory sessionFactory;
    protected final HibernateOmniSearch omniSearch;
    protected final RSQLParser rsqlParser;

    /**
     * Creates a Hibernate Reactive CRUD provider with custom event handling and search configuration.
     */
    public HibernateCrudProvider(Mutiny.SessionFactory session, HibernateOmniSearch omniSearch, RSQLParser rsqlParser, Class<ENTITY> entityClass, MutinyCrudEvents<ENTITY, ID, INPUT> crudEvents) {
        super(entityClass, crudEvents);
        this.sessionFactory = session;
        this.omniSearch = omniSearch;
        this.rsqlParser = rsqlParser;
    }

    /**
     * Creates a Hibernate Reactive CRUD provider using default {@link HibernateOmniSearch} and {@link RSQLParser}.
     */
    public HibernateCrudProvider(Mutiny.SessionFactory sessionFactory, Class<ENTITY> entityClass, MutinyCrudEvents<ENTITY, ID, INPUT> crudEvents) {
        this(sessionFactory, new HibernateOmniSearch(sessionFactory), new RSQLParser(), entityClass, crudEvents);
    }

    /**
     * Creates a Hibernate Reactive CRUD provider using default events, search, and parser.
     */
    public HibernateCrudProvider(Mutiny.SessionFactory sessionFactory, Class<ENTITY> entityClass) {
        this(sessionFactory, new HibernateOmniSearch(sessionFactory), new RSQLParser(), entityClass, MutinyCrudEvents.getDefault());
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    /**
     * Retrieves a paginated list of entities.
     */
    protected Uni<Page<ENTITY>> internalPage(Pagination pagination, Sort sort) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .pagination(pagination)
                .sort(sort));
    }

    /**
     * Searches entities using a search string with pagination and sorting.
     */
    protected Uni<Page<ENTITY>> internalSearch(String search, Pagination pagination, Sort sort) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort));
    }

    /**
     * Searches entities using a search string and RSQL query with pagination and sorting.
     */
    protected Uni<Page<ENTITY>> internalSearch(String search, Pagination pagination, Sort sort, String query) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort)
                .query(query));
    }

    /**
     * Finds an entity by its identifier or fails with {@link NotFoundEntityException}.
     */
    protected Uni<ENTITY> internalFind(ID id) {
        return sessionFactory.withSession(session -> session.find(entityClass, id))
                .onItem().ifNull().failWith(() -> new NotFoundEntityException(entityClass, id));
    }

    /**
     * Counts entities matching search string and RSQL query.
     */
    protected Uni<Long> internalCount(String search, String query) {
        return omniSearch.count(entityClass, new OmniSearchOptions()
                .search(search)
                .query(query));
    }

    /**
     * Counts entities matching search string.
     */
    protected Uni<Long> internalCount(String search) {
        return omniSearch.count(entityClass, new OmniSearchOptions()
                .search(search));
    }

    /**
     * Counts all entities.
     */
    @Override
    protected Uni<Long> internalCount() {
        return sessionFactory.withSession(session -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(entityClass);
            cq.select(cb.count(root));
            return session.createQuery(cq).getSingleResult();
        });
    }

    /**
     * Checks if an entity exists by its identifier.
     */
    @Override
    protected Uni<Boolean> internalExists(ID id) {
        return sessionFactory.withSession(session -> {
            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(entityClass);
            var idFieldName = getEntityIdFieldName();
            cq.select(cb.count(root)).where(cb.equal(root.get(idFieldName), id));
            return session.createQuery(cq).getSingleResult()
                    .onItem().transform(count -> count != null && count > 0);
        });
    }

    /**
     * Persists a new entity.
     */
    @Override
    protected Uni<ENTITY> internalCreate(ENTITY entity) {
        return sessionFactory.withSession(session -> session
                .persist(entity)
                .replaceWith(entity)
        );
    }

    /**
     * Updates an existing entity.
     */
    @Override
    protected Uni<ENTITY> internalUpdate(ENTITY entity) {
        return sessionFactory.withSession(session -> session.merge(entity));
    }

    /**
     * Deletes an entity.
     */
    @Override
    protected Uni<Void> internalDelete(ENTITY entity) {
        return sessionFactory.withSession(session -> session
                .remove(entity)
                .replaceWithVoid()
        );
    }

    /**
     * Executes a function within a transaction.
     */
    @Override
    protected <T> Uni<T> withTransaction(Supplier<Uni<T>> function) {
        return sessionFactory.withTransaction((session, tx) -> function.get());
    }

    /**
     * Retrieves the name of the entity identifier field.
     * Can be overridden if the ID field is not named conventionally.
     */
    protected String getEntityIdFieldName() {
        return InternalUtils.getIdFieldName(entityClass);
    }
}
