package com.peluware.freddy.cruder.reactive.mutiny;

import com.peluware.freddy.cruder.CrudOperation;
import io.smallrye.mutiny.Uni;

/**
 * Reactive support interface providing hooks for preprocessing and postprocessing around CRUD operations.
 * <p>
 * Implementations can override the default methods to add custom behavior such as validation,
 * logging, security checks, or auditing before and after CRUD operations in a reactive way.
 * </p>
 */
public interface MutinyCrudProcessSupport {

    /**
     * Reactive preprocess hook invoked before each CRUD operation.
     * Implementations may override to add validation, logging, security, etc.
     *
     * @param operation the CRUD operation about to be executed
     * @return a {@link Uni} completing when preprocessing is done
     */
    default Uni<Void> preProcess(CrudOperation operation) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Reactive postprocess hook invoked after each CRUD operation.
     * Implementations may override to add logging, auditing, etc.
     *
     * @param operation the CRUD operation that was executed
     * @return a {@link Uni} completing when postprocessing is done
     */
    default Uni<Void> postProcess(CrudOperation operation) {
        return Uni.createFrom().voidItem();
    }
}
