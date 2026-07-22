package com.peluware.freddy.cruder.jpa.query;

import com.peluware.freddy.cruder.jpa.JpaUtils;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Metamodel;
import org.jspecify.annotations.Nullable;

/**
 * Builds the {@code WHERE} predicate of a Criteria query from the query source (a {@code Root}
 * or a {@code Join}) and the {@link CriteriaBuilder}. Compose with {@link #and}/{@link #or};
 * return {@code null} (or {@link #all()}) for no restriction.
 *
 * <pre>{@code
 * JpaPredicate<Product> byActiveName =
 *     (from, cb) -> cb.and(cb.isTrue(from.get("active")), cb.equal(from.get("name"), name));
 * }</pre>
 *
 * @param <ENTITY> the entity type of the query source
 */
@FunctionalInterface
public interface JpaPredicate<ENTITY> {

    @Nullable Predicate build(From<?, ENTITY> from, CriteriaBuilder cb);

    /**
     * Combines this predicate with {@code other} using a logical {@code AND}. A {@code null}
     * side is treated as an absent term and skipped, so combining with a {@link #all()} (or any
     * predicate that yields {@code null}) returns the other side unchanged.
     *
     * @param other the predicate to conjoin
     * @return a predicate that holds when both present sides hold
     */
    default JpaPredicate<ENTITY> and(JpaPredicate<ENTITY> other) {
        return (from, cb) -> {
            Predicate a = build(from, cb);
            Predicate b = other.build(from, cb);
            if (a == null) return b;
            if (b == null) return a;
            return cb.and(a, b);
        };
    }

    /**
     * Combines this predicate with {@code other} using a logical {@code OR}. A {@code null}
     * side is treated as an absent term and skipped.
     *
     * @param other the predicate to disjoin
     * @return a predicate that holds when either present side holds
     */
    default JpaPredicate<ENTITY> or(JpaPredicate<ENTITY> other) {
        return (from, cb) -> {
            Predicate a = build(from, cb);
            Predicate b = other.build(from, cb);
            if (a == null) return b;
            if (b == null) return a;
            return cb.or(a, b);
        };
    }

    /**
     * A predicate that applies no restriction (yields {@code null}). Useful as the neutral
     * element for {@link #and}/{@link #or} and where a query needs no {@code WHERE} clause.
     *
     * @param <ENTITY> the entity type of the query source
     * @return a predicate that never restricts the query
     */
    static <ENTITY> JpaPredicate<ENTITY> all() {
        return (_, _) -> null;
    }

    /**
     * Matches the entity by its identifier. Supports a single id attribute, including
     * {@code @EmbeddedId}; the attribute name is resolved from the {@code metamodel}.
     *
     * @param metamodel the JPA metamodel, used to resolve the identifier attribute
     * @param id        the identifier value to match
     * @param <ENTITY>  the entity type of the query source
     * @return a predicate matching the identifier
     */
    static <ENTITY> JpaPredicate<ENTITY> byId(Metamodel metamodel, Object id) {
        return (from, cb) -> cb.equal(from.get(JpaUtils.getIdFieldName(metamodel, from.getJavaType())), id);
    }
}
