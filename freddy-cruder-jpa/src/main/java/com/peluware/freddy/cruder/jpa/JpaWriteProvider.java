package com.peluware.freddy.cruder.jpa;

import com.peluware.freddy.cruder.annotations.Final;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.WriteProviderSupport;

import java.util.Optional;

public interface JpaWriteProvider<E, D, ID> extends
        EntityManagerSupplier,
        WriteProviderSupport<E, D, ID> {

    @Override
    @Protected
    @Final
    default E internalFind(ID id) throws NotFoundEntityException {
        var em = getEntityManager();
        var entityClass = getEntityClass();
        return Optional.ofNullable(em.find(entityClass, id))
                .orElseThrow(() -> new NotFoundEntityException(entityClass, id));
    }

    @Override
    @Protected
    @Final
    default E internalCreate(E entity) {
        var em = getEntityManager();
        em.persist(entity);
        return entity;
    }

    @Override
    @Protected
    @Final
    default E internalUpdate(E entity) {
        var em = getEntityManager();
        return em.merge(entity);
    }

    @Override
    @Protected
    @Final
    default void internalDelete(E entity) {
        var em = getEntityManager();
        em.remove(entity);
    }
}
