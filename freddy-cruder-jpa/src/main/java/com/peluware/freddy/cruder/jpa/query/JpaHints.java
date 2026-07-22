package com.peluware.freddy.cruder.jpa.query;

import jakarta.persistence.CacheRetrieveMode;
import jakarta.persistence.CacheStoreMode;
import jakarta.persistence.EntityGraph;

import java.util.Map;

/**
 * Standard Jakarta Persistence query hints, as named constants and single-hint factories, for
 * {@link JpaQuery#addHints(Map)} and for {@code getQueryHints()} overrides in the providers.
 *
 * <pre>{@code
 * // one hint
 * new EntityFindQuery<>(Product.class, byId, onMissing)
 *     .addHints(JpaHints.fetchGraph(graph))
 *     .exec(entityManager);
 *
 * // several — compose with the constants
 * query.addHints(Map.of(
 *     JpaHints.FETCH_GRAPH, graph,
 *     JpaHints.QUERY_TIMEOUT, 500));
 * }</pre>
 *
 * <p>Only hints defined by the Jakarta Persistence specification are listed here, so they work on
 * any provider. For vendor-specific hints (e.g. {@code org.hibernate.readOnly}) use that vendor's
 * own constants.</p>
 */
public final class JpaHints {

    /** Entity graph applied as a fetch graph: its attributes are EAGER, every other one LAZY. */
    public static final String FETCH_GRAPH = "jakarta.persistence.fetchgraph";

    /** Entity graph applied as a load graph: its attributes are EAGER, the rest keep their mapping. */
    public static final String LOAD_GRAPH = "jakarta.persistence.loadgraph";

    /** Query timeout, in milliseconds. */
    public static final String QUERY_TIMEOUT = "jakarta.persistence.query.timeout";

    /** Pessimistic lock timeout, in milliseconds. */
    public static final String LOCK_TIMEOUT = "jakarta.persistence.lock.timeout";

    /** How the second-level cache is read — a {@link CacheRetrieveMode}. */
    public static final String CACHE_RETRIEVE_MODE = "jakarta.persistence.cache.retrieveMode";

    /** How the second-level cache is written — a {@link CacheStoreMode}. */
    public static final String CACHE_STORE_MODE = "jakarta.persistence.cache.storeMode";

    private JpaHints() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Fetches the given entity graph: its attributes are loaded EAGER, every other one LAZY.
     *
     * @param graph the entity graph to fetch
     * @return the hint map
     */
    public static Map<String, Object> fetchGraph(EntityGraph<?> graph) {
        return Map.of(FETCH_GRAPH, graph);
    }

    /**
     * Loads the given entity graph: its attributes are loaded EAGER, the rest keep their mapping.
     *
     * @param graph the entity graph to load
     * @return the hint map
     */
    public static Map<String, Object> loadGraph(EntityGraph<?> graph) {
        return Map.of(LOAD_GRAPH, graph);
    }

    /**
     * Caps how long the query may run.
     *
     * @param milliseconds the timeout in milliseconds
     * @return the hint map
     */
    public static Map<String, Object> queryTimeout(int milliseconds) {
        return Map.of(QUERY_TIMEOUT, milliseconds);
    }

    /**
     * Caps how long a pessimistic lock is waited for.
     *
     * @param milliseconds the timeout in milliseconds
     * @return the hint map
     */
    public static Map<String, Object> lockTimeout(int milliseconds) {
        return Map.of(LOCK_TIMEOUT, milliseconds);
    }

    /**
     * Sets how the second-level cache is read.
     *
     * @param mode the retrieve mode
     * @return the hint map
     */
    public static Map<String, Object> cacheRetrieveMode(CacheRetrieveMode mode) {
        return Map.of(CACHE_RETRIEVE_MODE, mode);
    }

    /**
     * Sets how the second-level cache is written.
     *
     * @param mode the store mode
     * @return the hint map
     */
    public static Map<String, Object> cacheStoreMode(CacheStoreMode mode) {
        return Map.of(CACHE_STORE_MODE, mode);
    }
}
