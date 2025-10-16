package com.peluware.freddy.cruder.jpa;

import com.peluware.freddy.cruder.annotations.Final;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.WriteProviderSupport;

public interface JpaWriteProvider<E, D, ID> extends
        EntityManagerSupplier,
        WriteProviderSupport<E, D, ID> {

    @Override
    @Protected
    @Final
    default E internalFind(ID id) throws NotFoundEntityException {
        var em = getEntityManager();
        var entityClass = getEntityClass();
        E entity = em.find(entityClass, id);
        if (entity == null) {
            throw new NotFoundEntityException(entityClass, id);
        }
        return entity;
    }

    @Override
    @Protected
    @Final
    default void internalCreate(E entity) {
        var em = getEntityManager();
        em.persist(entity);
    }

    @Override
    @Protected
    @Final
    default void internalUpdate(E entity) {
        var em = getEntityManager();
        em.merge(entity);
    }

    @Override
    @Protected
    @Final
    default void internalDelete(E entity) {
        var em = getEntityManager();
        em.remove(entity);
    }
}
