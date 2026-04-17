package com.peluware.freddy.cruder;


public class NotFoundException extends RuntimeException {

    private final transient Object id;

    public NotFoundException(Object id, String message) {
        super(message);
        this.id = id;
    }

    public NotFoundException(Object id) {
        this(id, "Not found target with id [" + id + "]");
    }

    public Object getId() {
        return id;
    }
}
