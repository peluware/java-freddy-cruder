package com.peluware.freddy.cruder;

import java.lang.ScopedValue;
import java.util.function.Supplier;

public record CrudContext(CrudOptions options) {

    private static final ScopedValue<CrudContext> CONTEXT = ScopedValue.newInstance();

    public static CrudContext current() {
        if (!CONTEXT.isBound()) {
            throw new IllegalStateException("No CrudContext is currently bound to this scope. Ensure that you are running within a scope where a CrudContext is set.");
        }
        return CONTEXT.get();
    }

    public static boolean hasContext() {
        return CONTEXT.isBound();
    }

    public static <T> T call(CrudContext context, Supplier<T> action) {
        return ScopedValue.where(CONTEXT, context).call(action::get);
    }

    public static <T> T call(CrudOptions options, Supplier<T> action) {
        return call(new CrudContext(options), action);
    }

    public static void run(CrudContext context, Runnable action) {
        ScopedValue.where(CONTEXT, context).run(action);
    }

    public static void run(CrudOptions options, Runnable action) {
        run(new CrudContext(options), action);
    }


}