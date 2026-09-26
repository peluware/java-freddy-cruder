package com.peluware.freddy.cruder.bulkimport.excel;

import org.jspecify.annotations.NullUnmarked;

import java.time.LocalDate;

@NullUnmarked
public class Person {

    Long id;
    String name;
    Integer age;
    LocalDate birthDate;
    Role role;

    public Person() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
