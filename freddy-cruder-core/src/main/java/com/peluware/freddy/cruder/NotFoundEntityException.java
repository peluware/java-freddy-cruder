package com.peluware.freddy.cruder;


public class NotFoundEntityException extends NotFoundException {

    private final Class<?> entityClass;

    public NotFoundEntityException(Class<?> entityClass, Object id, String message) {
        super(id, message);
        this.entityClass = entityClass;
    }

    public NotFoundEntityException(Class<?> entityClass, Object id) {
        this(entityClass, id, "Not found entity with id [" + id + "]");
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

}
