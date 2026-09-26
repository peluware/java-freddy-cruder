package com.peluware.freddy.cruder.bulkimport.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Function;

/**
 * A cell of a row, read through typed accessors; a cell that does not exist reads as blank.
 */
public final class ExcelCell {

    private final @Nullable Cell cell;
    private final @Nullable FormulaEvaluator evaluator;
    private final boolean date1904;

    ExcelCell(@Nullable Cell cell, @Nullable FormulaEvaluator evaluator, boolean date1904) {
        this.cell = cell;
        this.evaluator = evaluator;
        this.date1904 = date1904;
    }

    /**
     * Reads the cell as trimmed text; numbers have no trailing zeros, dates and times are ISO.
     *
     * @return the text, empty if the cell is empty or holds an error
     */
    public String text() {
        var value = value();
        if (value == null) {
            return "";
        }
        return switch (value.getCellType()) {
            case STRING -> clean(value.getStringValue());
            case NUMERIC -> numericText(value.getNumberValue());
            case BOOLEAN -> Boolean.toString(value.getBooleanValue());
            default -> "";
        };
    }

    /**
     * @return the text of the cell, or {@code null} if it is blank
     */
    public @Nullable String textOrNull() {
        var text = text();
        return text.isEmpty() ? null : text;
    }

    /**
     * Checks whether the text of the cell is any of {@code truthy}, ignoring case.
     *
     * @param truthy the values that count as true
     * @return {@code true} if the cell holds one of them
     */
    public boolean flag(String... truthy) {
        var text = text();
        for (var candidate : truthy) {
            if (text.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the number in the cell, or {@code null} if it does not hold one
     */
    public @Nullable Double number() {
        var value = value();
        return value != null && value.getCellType() == CellType.NUMERIC
            ? value.getNumberValue()
            : null;
    }

    /**
     * @return the number in the cell as an integer, or {@code null} if it does not hold one
     */
    public @Nullable Integer integer() {
        var number = number();
        return number == null ? null : number.intValue();
    }

    /**
     * @return the number in the cell as a long, or {@code null} if it does not hold one
     */
    public @Nullable Long longValue() {
        var number = number();
        return number == null ? null : number.longValue();
    }

    /**
     * @return the number in the cell as a decimal, or {@code null} if it does not hold one
     */
    public @Nullable BigDecimal decimal() {
        var number = number();
        return number == null ? null : BigDecimal.valueOf(number);
    }

    /**
     * @return the value of a boolean cell, or {@code null} if it does not hold one
     */
    public @Nullable Boolean bool() {
        var value = value();
        return value != null && value.getCellType() == CellType.BOOLEAN
            ? value.getBooleanValue()
            : null;
    }

    /**
     * @return the date in the cell, or {@code null} if it does not hold one
     */
    public @Nullable LocalDate date() {
        var dateTime = dateTime();
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    /**
     * @return the date and time in the cell, or {@code null} if it does not hold one
     */
    public @Nullable LocalDateTime dateTime() {
        var number = number();
        return number != null && isDate() ? DateUtil.getLocalDateTime(number, date1904) : null;
    }

    /**
     * @return the time of day in the cell, or {@code null} if it does not hold a date or a time
     */
    public @Nullable LocalTime time() {
        var dateTime = dateTime();
        return dateTime == null ? null : dateTime.toLocalTime();
    }

    /**
     * Reads the date and time in the cell as a moment in a time zone.
     *
     * @param zone the time zone of the date and time in the cell
     * @return the moment in the cell, or {@code null} if it does not hold a date
     */
    public @Nullable Instant instant(ZoneId zone) {
        var zoned = zoned(zone);
        return zoned == null ? null : zoned.toInstant();
    }

    /**
     * @param zone the time zone of the date and time in the cell
     * @return the date and time at {@code zone}, or {@code null} if the cell does not hold a date
     */
    public @Nullable ZonedDateTime zoned(ZoneId zone) {
        var dateTime = dateTime();
        return dateTime == null ? null : dateTime.atZone(zone);
    }

    /**
     * Reads the enum constant whose name is the text of the cell, ignoring case.
     *
     * @param type the enum type
     * @param <E>  the enum type
     * @return the constant, or {@code null} if the cell is blank
     * @throws IllegalArgumentException if the text is not the name of any constant
     */
    public <E extends Enum<E>> @Nullable E enumeration(Class<E> type) {
        var text = textOrNull();
        if (text == null) {
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
     * Converts the text of the cell with a parser.
     *
     * @param parser converts a non-blank text; its exceptions propagate
     * @param <T>    the type to convert to
     * @return the converted value, or {@code null} if the cell is blank
     */
    public <T> @Nullable T textAs(Function<String, T> parser) {
        var text = textOrNull();
        return text == null ? null : parser.apply(text);
    }

    /**
     * @return whether the cell is empty
     */
    public boolean blank() {
        return text().isEmpty();
    }

    /**
     * @return the underlying Apache POI cell, or {@code null} if the cell does not exist
     */
    public @Nullable Cell raw() {
        return cell;
    }

    private @Nullable CellValue value() {
        if (cell == null) {
            return null;
        }
        if (evaluator == null) {
            return stored(cell);
        }
        try {
            return evaluator.evaluate(cell);
        } catch (RuntimeException e) {
            return stored(cell);
        }
    }

    private static @Nullable CellValue stored(Cell cell) {
        var type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        return switch (type) {
            case NUMERIC -> new CellValue(cell.getNumericCellValue());
            case STRING -> new CellValue(cell.getStringCellValue());
            case BOOLEAN -> CellValue.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private static String clean(String text) {
        return text.replace(' ', ' ').replace(' ', ' ').trim();
    }

    private boolean isDate() {
        return DateUtil.isCellDateFormatted(cell);
    }

    private String numericText(double number) {
        if (isDate()) {
            var dateTime = DateUtil.getLocalDateTime(number, date1904);
            if (number >= 0 && number < 1) {
                return dateTime.toLocalTime().toString();
            }
            return dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? dateTime.toLocalDate().toString()
                : dateTime.toString();
        }
        return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
    }
}
