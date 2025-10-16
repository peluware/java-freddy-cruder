package com.peluware.freddy.cruder.jpa;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.annotations.Final;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.EntityClassSupplier;
import com.peluware.freddy.cruder.ReadProviderSupport;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.jpa.JpaOmniSearch;
import cz.jirutka.rsql.parser.RSQLParser;

import java.util.List;

public interface JpaReadProvider<E, ID> extends
        EntityManagerSupplier,
        EntityClassSupplier<E>,
        ReadProviderSupport<E, ID> {

    RSQLParser DEFAULT_RSQL_PARSER = new RSQLParser();


    @Protected
    default JpaOmniSearch getOmniSerach() {
        return new JpaOmniSearch(getEntityManager());
    }

    @Protected
    default RSQLParser getRsqlParser() {
        return DEFAULT_RSQL_PARSER;
    }

    @Override
    @Protected
    @Final
    default Page<E> internalPage(Pagination pagination, Sort sort) {
        return getOmniSerach().page(getEntityClass(), new OmniSearchOptions()
                .pagination(pagination)
                .sort(sort));
    }

    @Override
    @Protected
    @Final
    default Page<E> internalSearch(String search, Pagination pagination, Sort sort) {
        return getOmniSerach().page(getEntityClass(), new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort));
    }

    @Override
    @Protected
    @Final
    default Page<E> internalSearch(String search, Pagination pagination, Sort sort, String query) {
        return getOmniSerach().page(getEntityClass(), new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort)
                .query(getRsqlParser().parse(query)));
    }

    @Override
    @Protected
    @Final
    default E internalFind(ID id) throws NotFoundEntityException {
        var entityClass = getEntityClass();
        var entity = getEntityManager().find(entityClass, id);
        if (entity == null) {
            throw new NotFoundEntityException(entityClass, id);
        }
        return entity;
    }

    @Override
    @Protected
    @Final
    default List<E> internalFind(List<ID> ids) {
        var em = getEntityManager();
        var entityClass = getEntityClass();

        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(entityClass);
        var root = cq.from(entityClass);

        var idFieldName = JpaUtils.getIdFieldName(entityClass);

        cq.where(root.get(idFieldName).in(ids));

        return em.createQuery(cq).getResultList();
    }

    @Override
    @Protected
    @Final
    default long internalCount(String search, String query) {
        return getOmniSerach().count(getEntityClass(), opt -> opt
                .search(search)
                .query(getRsqlParser().parse(query)));
    }

    @Override
    @Protected
    @Final
    default long internalCount(String search) {
        return getOmniSerach().count(getEntityClass(), opt -> opt
                .search(search));
    }

    @Override
    @Protected
    @Final
    default long internalCount() {
        var em = getEntityManager();
        var entityClass = getEntityClass();

        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(entityClass);

        cq.select(cb.count(root));

        return em.createQuery(cq).getSingleResult();
    }

    @Override
    @Protected
    @Final
    default boolean internalExists(ID id) {

        var em = getEntityManager();
        var entityClass = getEntityClass();

        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(Long.class);
        var root = cq.from(entityClass);

        var idFieldName = JpaUtils.getIdFieldName(entityClass);

        cq.select(cb.count(root))
                .where(cb.equal(root.get(idFieldName), id));

        var count = em.createQuery(cq).getSingleResult();
        return count != null && count > 0;
    }
}
