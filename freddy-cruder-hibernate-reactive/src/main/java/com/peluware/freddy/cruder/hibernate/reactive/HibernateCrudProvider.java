package com.peluware.freddy.cruder.hibernate.reactive;

import com.peluware.freddy.cruder.annotations.Final;
import com.peluware.freddy.cruder.annotations.Protected;
import com.peluware.freddy.cruder.reactive.mutiny.MutinyCrudProviderSupport;
import io.smallrye.mutiny.Uni;

public interface HibernateCrudProvider<E, D, ID> extends
        HibernateReadProvider<E, ID>,
        HibernateWriteProvider<E, D, ID>,
        MutinyCrudProviderSupport<E, D, ID> {

    @Override
    @Protected
    @Final
    default Uni<E> internalFind(ID id) {
        return HibernateReadProvider.super.internalFind(id);
    }
}
