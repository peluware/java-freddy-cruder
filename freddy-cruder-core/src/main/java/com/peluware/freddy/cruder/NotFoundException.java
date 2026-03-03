package com.peluware.freddy.cruder;


public class NotFoundException extends RuntimeException {
    private final transient Object id;

    public NotFoundException(Object id) {
        this.id = id;
    }

    public Object getId() {
        return id;
    }
}
