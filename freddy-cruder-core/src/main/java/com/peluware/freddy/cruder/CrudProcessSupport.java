package com.peluware.freddy.cruder;

/**
 * Support interface providing hooks for preprocessing and postprocessing around CRUD operations.
 * <p>
 * Implementations can override the default methods to add custom behavior such as validation,
 * logging, security checks, or auditing before and after CRUD operations.
 * </p>
 */
public interface CrudProcessSupport {
    /**
     * Preprocess hook invoked before each CRUD operation.
     * Implementations may override to add validation, logging, security, etc.
     *
     * @param operation the CRUD operation about to be executed
     */
    default void preProcess(CrudOperation operation) {
        // IMPLEMENT IF NEEDED
    }

    /**
     * Postprocess hook invoked after each CRUD operation Implementations may override to add logging, auditing, etc.
     *
     * @param operation the CRUD operation that was executed
     */
    default void postProcess(CrudOperation operation) {
        // IMPLEMENT IF NEEDED
    }
}
