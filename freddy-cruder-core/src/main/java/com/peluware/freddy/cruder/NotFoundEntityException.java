package com.peluware.freddy.cruder;


public class NotFoundEntityException extends NotFoundException {
    private final Class<?> entityClass;

    public NotFoundEntityException(Class<?> entityClass, Object id) {
        super(id);
        this.entityClass = entityClass;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

}
