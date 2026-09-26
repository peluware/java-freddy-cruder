package com.peluware.freddy.cruder.memory;

import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Defines how an in-memory provider reads, generates and copies entity identifiers.
 *
 * @param getter    reads the identifier of an entity, or {@code null} if it has none yet
 * @param setter    assigns a generated identifier, or {@code null} if none is generated
 * @param generator produces the next identifier, or {@code null} if none is generated
 * @param copier    copies an entity, or {@code null} to hold entities by reference
 * @param <ENTITY>  the entity type
 * @param <ID>      the identifier type
 */
public record MemoryIds<ENTITY, ID>(
    Function<ENTITY, @Nullable ID> getter,
    @Nullable BiConsumer<ENTITY, ID> setter,
    @Nullable Supplier<ID> generator,
    @Nullable UnaryOperator<ENTITY> copier
) {

    public MemoryIds {
        Objects.requireNonNull(getter, "Getter must not be null");
        if ((setter == null) != (generator == null)) {
            throw new IllegalArgumentException("Setter and generator must be given together");
        }
    }

    /**
     * Creates ids read from the entity, which always carries one.
     *
     * @param getter   reads the identifier of an entity
     * @param <ENTITY> the entity type
     * @param <ID>     the identifier type
     * @return the identifiers
     */
    public static <ENTITY, ID> MemoryIds<ENTITY, ID> of(Function<ENTITY, @Nullable ID> getter) {
        return new MemoryIds<>(getter, null, null, null);
    }

    /**
     * Creates ids generated when an entity is stored without one.
     *
     * @param getter    reads the identifier of an entity
     * @param setter    assigns the generated identifier
     * @param generator produces the next identifier
     * @param <ENTITY>  the entity type
     * @param <ID>      the identifier type
     * @return the identifiers
     */
    public static <ENTITY, ID> MemoryIds<ENTITY, ID> generated(
        Function<ENTITY, @Nullable ID> getter,
        BiConsumer<ENTITY, ID> setter,
        Supplier<ID> generator
    ) {
        return new MemoryIds<>(getter, setter, generator, null);
    }

    /**
     * Returns the same ids with entities stored and read as copies, so a rollback also undoes changes
     * to their fields.
     *
     * @param copier copies an entity
     * @return the identifiers
     */
    public MemoryIds<ENTITY, ID> copying(UnaryOperator<ENTITY> copier) {
        return new MemoryIds<>(getter, setter, generator, Objects.requireNonNull(copier, "Copier must not be null"));
    }

    /**
     * Creates sequential {@code Long} ids starting at {@code 1}.
     *
     * @param getter   reads the identifier of an entity
     * @param setter   assigns the generated identifier
     * @param <ENTITY> the entity type
     * @return the identifiers
     */
    public static <ENTITY> MemoryIds<ENTITY, Long> sequentialLong(Function<ENTITY, @Nullable Long> getter, BiConsumer<ENTITY, Long> setter) {
        var sequence = new AtomicLong();
        return generated(getter, setter, sequence::incrementAndGet);
    }

    /**
     * Creates sequential {@code Integer} ids starting at {@code 1}.
     *
     * @param getter   reads the identifier of an entity
     * @param setter   assigns the generated identifier
     * @param <ENTITY> the entity type
     * @return the identifiers
     */
    public static <ENTITY> MemoryIds<ENTITY, Integer> sequentialInt(Function<ENTITY, @Nullable Integer> getter, BiConsumer<ENTITY, Integer> setter) {
        var sequence = new AtomicInteger();
        return generated(getter, setter, sequence::incrementAndGet);
    }

    /**
     * Creates sequential {@code BigInteger} ids starting at {@code 1}.
     *
     * @param getter   reads the identifier of an entity
     * @param setter   assigns the generated identifier
     * @param <ENTITY> the entity type
     * @return the identifiers
     */
    public static <ENTITY> MemoryIds<ENTITY, BigInteger> sequentialBigInteger(Function<ENTITY, @Nullable BigInteger> getter, BiConsumer<ENTITY, BigInteger> setter) {
        var sequence = new AtomicLong();
        return generated(getter, setter, () -> BigInteger.valueOf(sequence.incrementAndGet()));
    }
}
