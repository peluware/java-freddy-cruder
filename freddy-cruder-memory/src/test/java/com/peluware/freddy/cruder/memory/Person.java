package com.peluware.freddy.cruder.memory;

import org.jspecify.annotations.NullUnmarked;

@NullUnmarked
public class Person {

    Long id;
    String name;
    int age;

    public Person() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
