package com.peluware.freddy.cruder.hibernate.reactive;

import com.peluware.freddy.cruder.annotations.Final;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.reactive.mutiny.MutinyWriteProviderSupport;
import io.smallrye.mutiny.Uni;

import java.util.function.Supplier;

public interface HibernateWriteProvider<E, D, ID> extends
        HibernateSessionFactorySupplier,
        MutinyWriteProviderSupport<E, D, ID> {

    @Override
    @Protected
    @Final
    default Uni<E> internalFind(ID id) {
        return getSessionFactory().withSession(session -> session
                .find(getEntityClass(), id)
        );
    }

    @Override
    @Protected
    @Final
    default Uni<E> internalCreate(E entity) {
        return getSessionFactory().withSession(session -> session
                .persist(entity)
                .replaceWith(entity)
        );
    }

    @Override
    @Protected
    @Final
    default Uni<E> internalUpdate(E entity) {
        return getSessionFactory().withSession(session -> session
                .merge(entity)
        );
    }

    @Override
    @Protected
    @Final
    default Uni<E> internalDelete(E entity) {
        return getSessionFactory().withSession(session -> session
                .remove(entity)
                .replaceWith(entity)
        );
    }

    @Override
    @Protected
    default <T> Uni<T> withTransaction(Supplier<Uni<T>> function) {
        return getSessionFactory().withTransaction((session, tx) -> function.get());
    }
}
