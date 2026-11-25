package com.peluware.freddy.cruder;

/**
 * Marks a CRUD provider as read-only: its {@code create}, {@code update} and {@code delete} throw
 * {@link ReadOnlyException} instead of running.
 *
 * <p>Each provider has a nested {@code ReadOnly} class that already is one, with {@code Void} as
 * its input and {@code mapInput} implemented:</p>
 *
 * <pre>{@code
 * public class ProductQueries extends FilterableJpaCrudProvider.ReadOnly<Product, Long, ProductOutput> {
 *     // only mapOutput and predicateFilter
 * }
 * }</pre>
 */
public interface ReadOnlyProvider {

    /**
     * Throws {@link ReadOnlyException} if {@code provider} is a {@link ReadOnlyProvider}. Called by
     * the write operations of the providers of this library.
     *
     * @param provider the provider about to write
     */
    static void rejectWrite(Object provider) {
        if (provider instanceof ReadOnlyProvider) {
            throw new ReadOnlyException(provider.getClass());
        }
    }
}
