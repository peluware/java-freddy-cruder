package com.peluware.freddy.cruder;


public class NotFoundEntityException extends RuntimeException {
    private final Class<?> entityClass;
    private final transient Object id;

    public NotFoundEntityException(Class<?> entityClass, Object id) {
        this.entityClass = entityClass;
        this.id = id;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public Object getId() {
        return id;
    }


}
