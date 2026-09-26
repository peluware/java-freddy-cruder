package com.peluware.freddy.cruder.bulkimport.csv;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;

/**
 * A value of a CSV record, with typed accessors that return {@code null} if it is blank.
 */
public final class CsvCell {

    private final String text;

    CsvCell(String text) {
        this.text = text;
    }

    /**
     * @return the trimmed text of the value, empty if it is empty or the record is shorter
     */
    public String text() {
        return text;
    }

    /**
     * @return the text of the value, or {@code null} if it is blank
     */
    public @Nullable String textOrNull() {
        return text.isEmpty() ? null : text;
    }

    /**
     * Whether the text of the value is any of {@code truthy}, ignoring case.
     *
     * @param truthy the values that count as true
     * @return whether the value is one of them
     */
    public boolean flag(String... truthy) {
        for (var candidate : truthy) {
            if (text.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the number, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not a number
     */
    public @Nullable Double number() {
        return parse("number", value -> new BigDecimal(value).doubleValue());
    }

    /**
     * @return the number as an integer, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not a whole number that fits
     */
    public @Nullable Integer integer() {
        return parse("integer", value -> new BigDecimal(value).intValueExact());
    }

    /**
     * @return the number as a long, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not a whole number that fits
     */
    public @Nullable Long longValue() {
        return parse("integer", value -> new BigDecimal(value).longValueExact());
    }

    /**
     * Reads the value as an exact decimal number.
     *
     * @return the number, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not a number
     */
    public @Nullable BigDecimal decimal() {
        return parse("number", BigDecimal::new);
    }

    /**
     * @return {@code true} or {@code false} for those texts, ignoring case, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is neither
     */
    public @Nullable Boolean bool() {
        return parse("boolean", value -> switch (value.toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(value);
        });
    }

    /**
     * @return the ISO date, such as {@code 2026-03-05}, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not a date
     */
    public @Nullable LocalDate date() {
        return parse("date", LocalDate::parse);
    }

    /**
     * @param formatter the format of the date, such as {@code dd/MM/yyyy}
     * @return the date, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text does not match the format
     */
    public @Nullable LocalDate date(DateTimeFormatter formatter) {
        return parse("date", value -> LocalDate.parse(value, formatter));
    }

    /**
     * @return the ISO date and time, such as {@code 2026-03-05T14:30}, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not a date and time
     */
    public @Nullable LocalDateTime dateTime() {
        return parse("date and time", LocalDateTime::parse);
    }

    /**
     * @param formatter the format of the date and time
     * @return the date and time, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text does not match the format
     */
    public @Nullable LocalDateTime dateTime(DateTimeFormatter formatter) {
        return parse("date and time", value -> LocalDateTime.parse(value, formatter));
    }

    /**
     * @return the ISO time of day, such as {@code 14:30}, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not a time
     */
    public @Nullable LocalTime time() {
        return parse("time", LocalTime::parse);
    }

    /**
     * @param formatter the format of the time
     * @return the time of day, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text does not match the format
     */
    public @Nullable LocalTime time(DateTimeFormatter formatter) {
        return parse("time", value -> LocalTime.parse(value, formatter));
    }

    /**
     * Reads the value as a moment written with its own offset.
     *
     * @return the moment, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not an ISO date and time with an offset
     */
    public @Nullable Instant instant() {
        return parse("instant", value -> OffsetDateTime.parse(value).toInstant());
    }

    /**
     * Reads the value as an ISO date and time in a time zone.
     *
     * @param zone the time zone the ISO date and time are in
     * @return the moment, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not an ISO date and time
     */
    public @Nullable Instant instant(ZoneId zone) {
        var zoned = zoned(zone);
        return zoned == null ? null : zoned.toInstant();
    }

    /**
     * @param zone the time zone the ISO date and time are in
     * @return the date and time at {@code zone}, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not an ISO date and time
     */
    public @Nullable ZonedDateTime zoned(ZoneId zone) {
        var dateTime = dateTime();
        return dateTime == null ? null : dateTime.atZone(zone);
    }

    /**
     * @return the ISO date and time with an offset, or {@code null} if it is blank
     * @throws IllegalArgumentException if the text is not an ISO date and time with an offset
     */
    public @Nullable OffsetDateTime offsetDateTime() {
        return parse("date and time with offset", OffsetDateTime::parse);
    }

    /**
     * The constant of {@code type} whose name is the text, ignoring case.
     *
     * @param type the enum type
     * @param <E>  the enum type
     * @return the constant, or {@code null} if the value is blank
     * @throws IllegalArgumentException if the text is not the name of any constant
     */
    public <E extends Enum<E>> @Nullable E enumeration(Class<E> type) {
        if (text.isEmpty()) {
            return null;
        }
        for (var constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("'" + text + "' is not a valid " + type.getSimpleName());
    }

    /**
     * Converts the text with a custom parser.
     *
     * @param parser converts a non-blank text; its exceptions propagate
     * @param <T>    the type to convert to
     * @return the converted value, or {@code null} if the value is blank
     */
    public <T> @Nullable T textAs(Function<String, T> parser) {
        return text.isEmpty() ? null : parser.apply(text);
    }

    /**
     * @return whether the value is empty
     */
    public boolean blank() {
        return text.isEmpty();
    }

    private <T> @Nullable T parse(String what, Function<String, T> parser) {
        if (text.isEmpty()) {
            return null;
        }
        try {
            return parser.apply(text);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("'" + text + "' is not a valid " + what, e);
        }
    }
}
