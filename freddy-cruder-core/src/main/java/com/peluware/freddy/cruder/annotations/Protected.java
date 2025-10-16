package com.peluware.freddy.cruder.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark interface methods that are intended for internal
 * use only within default implementations and should not be called
 * from external code.
 * <p>
 * Serves as an equivalent to a "restricted abstract method," since
 * all interface methods in Java are public by default.
 * </p>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface Protected {
    String value() default "internal use only";
}
