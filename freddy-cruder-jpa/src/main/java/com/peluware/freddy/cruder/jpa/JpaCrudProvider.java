package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityCrudProvider;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.jpa.JpaOmniSearch;
import cz.jirutka.rsql.parser.RSQLParser;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * JPA-specific implementation of {@link EntityCrudProvider} that provides
 * standard CRUD operations for JPA entities using an {@link EntityManager}.
 *
 * <p>
 * This class integrates with {@link JpaOmniSearch} to support search and pagination
 * via the OmniSearch API, and uses {@link RSQLParser} for parsing optional RSQL queries.
 * </p>
 *
 * <p>
 * It provides default implementations for finding, counting, creating, updating, and deleting
 * entities, as well as checking existence. Subclasses can override any behavior as needed.
 * </p>
 *
 * @param <ENTITY> the JPA entity type
 * @param <ID>     the entity identifier type
 * @param <INPUT>  the input DTO type for create/update operations
 * @param <OUTPUT> the output DTO or projection type
 */
public abstract class JpaCrudProvider<ENTITY, ID, INPUT, OUTPUT> extends EntityCrudProvider<ENTITY, ID, INPUT, OUTPUT> {

    protected final EntityManager entityManager;
    protected final JpaOmniSearch omniSearch;

    /**
     * Creates a JPA CRUD provider with the given EntityManager, JpaOmniSearch, and entity class.
     */
    public JpaCrudProvider(EntityManager entityManager, JpaOmniSearch omniSearch, Class<ENTITY> entityClass) {
        super(entityClass);
        this.entityManager = entityManager;
        this.omniSearch = omniSearch;
    }

    /**
     * Creates a JPA CRUD provider with the given EntityManager and entity class,
     * using a default JpaOmniSearch with RSQL parsing.
     */
    public JpaCrudProvider(EntityManager entityManager, Class<ENTITY> entityClass) {
        this(entityManager, new JpaOmniSearch(entityManager, new RSQLParser()), entityClass);
    }

    // ------------------------------------------------------------
    // INTERNAL CRUD IMPLEMENTATIONS
    // ------------------------------------------------------------

    /**
     * Finds an entity by its identifier or throws {@link NotFoundEntityException} if not found.
     */
    @Override
    protected ENTITY internalFind(ID id) throws NotFoundEntityException {
        return Optional.ofNullable(entityManager.find(entityClass, id))
                .orElseThrow(() -> new NotFoundEntityException(entityClass, id));
    }

    /**
     * Retrieves a paginated list of entities.
     */
    @Override
    protected Page<ENTITY> internalPage(Pagination pagination, Sort sort) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .pagination(pagination)
                .sort(sort));
    }

    /**
     * Searches entities using a search string with pagination and sorting.
     */
    @Override
    protected Page<ENTITY> internalSearch(String search, Pagination pagination, Sort sort) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort));
    }

    /**
     * Searches entities using a search string and RSQL query with pagination and sorting.
     */
    @Override
    protected Page<ENTITY> internalSearch(String search, Pagination pagination, Sort sort, String query) {
        return omniSearch.page(entityClass, new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort)
                .query(query));
    }

    /**
     * Counts entities matching search string and RSQL query.
     */
    @Override
    protected long internalCount(String search, String query) {
        return omniSearch.count(entityClass, new OmniSearchOptions()
                .search(search)
                .query(query));
    }

    /**
     * Counts entities matching search string.
     */
    @Override
    protected long internalCount(String search) {
        return omniSearch.count(entityClass, new OmniSearchOptions()
                .search(search));
    }

    /**
     * Counts all entities.
     */
    @Override
    protected long internalCount() {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(entityClass);
        cq.select(cb.count(root));
        return entityManager.createQuery(cq).getSingleResult();
    }

    /**
     * Checks if an entity exists by its identifier.
     */
    @Override
    protected boolean internalExists(ID id) {
        var cb = entityManager.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(entityClass);
        var idFieldName = getEntityIdFieldName();
        cq.select(cb.count(root)).where(cb.equal(root.get(idFieldName), id));
        var count = entityManager.createQuery(cq).getSingleResult();
        return count != null && count > 0;
    }

    /**
     * Persists a new entity.
     */
    @Override
    protected ENTITY internalCreate(ENTITY entity) {
        entityManager.persist(entity);
        return entity;
    }

    /**
     * Updates an existing entity.
     */
    @Override
    protected ENTITY internalUpdate(ENTITY entity) {
        return entityManager.merge(entity);
    }

    /**
     * Deletes an entity.
     */
    @Override
    protected void internalDelete(ENTITY entity) {
        entityManager.remove(entity);
    }

    /**
     * Executes a function within a transaction.
     */
    @Override
    protected <T> T withTransaction(Supplier<T> function) {
        var transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = function.get();
            transaction.commit();
            return result;
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    /**
     * Retrieves the name of the entity identifier field.
     * Can be overridden if the ID field is not named conventionally.
     */
    protected String getEntityIdFieldName() {
        return InternalUtils.getIdFieldName(entityClass);
    }
}
