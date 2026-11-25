package com.peluware.freddy.cruder;

import java.util.function.Supplier;

public class NotFoundEntityException extends NotFoundException {

    private final Class<?> entityClass;

    public NotFoundEntityException(Class<?> entityClass, Object id, String message) {
        super(id, message);
        this.entityClass = entityClass;
    }

    public NotFoundEntityException(Class<?> entityClass, Object id) {
        this(entityClass, id, "Not found entity with id [" + id + "]");
    }

    /**
     * Returns a supplier of a {@link NotFoundEntityException} for {@code id}, for APIs that take the
     * exception to throw as a {@link Supplier}.
     *
     * @param entityClass the entity that was not found
     * @param id          the identifier that was looked up
     * @return a supplier that creates the exception on each call
     */
    public static Supplier<NotFoundEntityException> supplier(Class<?> entityClass, Object id) {
        return () -> new NotFoundEntityException(entityClass, id);
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

}
