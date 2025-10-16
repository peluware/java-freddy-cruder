package com.peluware.freddy.cruder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark interface methods that are intended to be
 * final and should not be overridden in implementing classes.
 * <p>
 * Serves as an equivalent to a "final abstract method," since
 * all interface methods in Java can be overridden by default.
 * </p>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface Final {
    String value() default "no override allowed";
}
