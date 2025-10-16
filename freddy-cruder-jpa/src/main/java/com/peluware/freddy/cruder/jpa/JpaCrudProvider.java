package com.peluware.freddy.cruder.jpa;

import com.peluware.freddy.cruder.annotations.Final;
import com.peluware.freddy.cruder.exceptions.NotFoundEntityException;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.CrudProviderSupport;

public interface JpaCrudProvider<E, D, ID> extends
        JpaReadProvider<E, ID>,
        JpaWriteProvider<E, D, ID>,
        CrudProviderSupport<E, D, ID> {

    @Override
    @Protected
    @Final
    default E internalFind(ID id) throws NotFoundEntityException {
        return JpaReadProvider.super.internalFind(id);
    }
}
