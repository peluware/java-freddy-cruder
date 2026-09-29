package com.peluware.freddy.cruder.springframework.web.export;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public class Person {

    Long id;
    String name;
    int age;

    public Person() {
    }

    Long getId() {
        return id;
    }

    void setId(Long id) {
        this.id = id;
    }
}
