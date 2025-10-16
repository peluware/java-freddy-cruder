package com.peluware.freddy.cruder.hibernate.reactive;

import com.peluware.domain.Page;
import com.peluware.domain.Pagination;
import com.peluware.domain.Sort;
import com.peluware.freddy.cruder.EntityClassSupplier;
import com.peluware.freddy.cruder.annotations.Final;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.reactive.mutiny.MutinyReadProviderSupport;
import com.peluware.omnisearch.OmniSearchOptions;
import com.peluware.omnisearch.hibernate.reactive.HibernateOmniSearch;
import cz.jirutka.rsql.parser.RSQLParser;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Objects;

interface HibernateReadProvider<E, ID> extends
        HibernateSessionFactorySupplier,
        EntityClassSupplier<E>,
        MutinyReadProviderSupport<E, ID> {

    RSQLParser DEFAULT_RSQL_PARSER = new RSQLParser();

    @Protected
    default HibernateOmniSearch getOmniSerach() {
        return new HibernateOmniSearch(getSessionFactory());
    }

    @Protected
    default RSQLParser getRsqlParser() {
        return DEFAULT_RSQL_PARSER;
    }

    @Override
    @Protected
    @Final
    default Uni<Page<E>> internalPage(Pagination pagination, Sort sort) {
        return getOmniSerach().page(getEntityClass(), new OmniSearchOptions()
                .pagination(pagination)
                .sort(sort));
    }

    @Override
    @Protected
    @Final
    default Uni<Page<E>> internalSearch(String search, Pagination pagination, Sort sort) {
        return getOmniSerach().page(getEntityClass(), new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort));
    }

    @Override
    @Protected
    @Final
    default Uni<Page<E>> internalSearch(String search, Pagination pagination, Sort sort, String query) {
        return getOmniSerach().page(getEntityClass(), new OmniSearchOptions()
                .search(search)
                .pagination(pagination)
                .sort(sort)
                .query(getRsqlParser().parse(query)));
    }

    @Override
    @Protected
    @Final
    default Uni<E> internalFind(ID id) {
        return getSessionFactory().withSession(session -> session.find(getEntityClass(), id));
    }

    @Override
    @Protected
    @Final
    default Uni<List<E>> internalFind(List<ID> ids) {
        return getSessionFactory().withSession(session -> {
            var entityClass = getEntityClass();

            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(entityClass);
            var root = cq.from(entityClass);

            var idFieldName = JpaUtils.getIdFieldName(entityClass);

            cq.where(root.get(idFieldName).in(ids));

            return session
                    .createQuery(cq)
                    .getResultList();

        });
    }

    @Override
    @Protected
    @Final
    default Uni<Long> internalCount(String search, String query) {
        return getOmniSerach().count(getEntityClass(), new OmniSearchOptions()
                .search(search)
                .query(getRsqlParser().parse(query)));
    }

    @Override
    @Protected
    @Final
    default Uni<Long> internalCount(String search) {
        return getOmniSerach().count(getEntityClass(), new OmniSearchOptions()
                .search(search));
    }

    @Override
    @Protected
    @Final
    default Uni<Long> internalCount() {
        return getSessionFactory().withSession(session -> {
            var entityClass = getEntityClass();

            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(entityClass);

            cq.select(cb.count(root));

            return session.createQuery(cq).getSingleResult();
        });
    }

    @Override
    @Protected
    @Final
    default Uni<Boolean> internalExists(ID id) {
        return getSessionFactory().withSession(session -> {
            Objects.requireNonNull(id, "The id must not be null");
            var entityClass = getEntityClass();

            var cb = session.getCriteriaBuilder();
            var cq = cb.createQuery(Long.class);
            var root = cq.from(entityClass);

            var idFieldName = JpaUtils.getIdFieldName(entityClass);

            cq.select(cb.count(root));
            cq.where(cb.equal(root.get(idFieldName), id));

            return session.createQuery(cq).getSingleResult()
                    .onItem().transform(count -> count != null && count > 0);
        });
    }
}
