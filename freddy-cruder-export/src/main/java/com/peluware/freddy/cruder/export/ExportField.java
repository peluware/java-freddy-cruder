package com.peluware.freddy.cruder.export;

import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * A property an export can include, and how to read it off a record. Not called a "column": a
 * field is a named, selectable piece of a record, independent of the destination format's shape.
 *
 * @param key   what identifies the field in a requested selection, such as {@code "name"}
 * @param label what a person reads for it, such as {@code "Name"}
 * @param value how to read the field off a record
 * @param <OUTPUT> the record type
 */
public record ExportField<OUTPUT>(String key, String label, Function<OUTPUT, @Nullable Object> value) {

    /**
     * @param record the record to read the field off
     * @return the field's value for that record
     */
    public @Nullable Object of(OUTPUT record) {
        return value.apply(record);
    }

    /**
     * Every field, or only the ones requested by key, in the order they are declared.
     *
     * @param fields    every field an export offers
     * @param requested the keys asked for; empty means every field
     * @return the fields to include
     * @throws UnknownExportFieldsException if a requested key isn't one of {@code fields}
     */
    public static <T> List<ExportField<T>> select(List<ExportField<T>> fields, List<String> requested) {
        if (requested.isEmpty()) {
            return fields;
        }

        var known = fields.stream().map(ExportField::key).toList();
        var unknown = requested.stream().filter(key -> !known.contains(key)).toList();

        if (!unknown.isEmpty()) {
            throw new UnknownExportFieldsException(unknown, known);
        }

        return fields.stream().filter(field -> requested.contains(field.key())).toList();
    }
}
