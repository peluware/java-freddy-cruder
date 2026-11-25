package com.peluware.freddy.cruder;

/**
 * Thrown when a write operation ({@code create}, {@code update} or {@code delete}) is invoked on a
 * {@link ReadOnlyProvider}.
 */
public class ReadOnlyException extends UnsupportedOperationException {

    private final transient Class<?> providerClass;

    public ReadOnlyException(Class<?> providerClass) {
        super(providerClass.getName() + " is read-only: create, update and delete are not supported");
        this.providerClass = providerClass;
    }

    public Class<?> getProviderClass() {
        return providerClass;
    }
}
