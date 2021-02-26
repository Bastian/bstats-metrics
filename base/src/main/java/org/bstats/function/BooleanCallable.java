package org.bstats.function;

import java.util.concurrent.Callable;

/**
 * A task that returns a result and may throw an exception.
 * Implementors define a single method with no arguments called
 * {@code call}.
 *
 * <p>The {@code BooleanCallable} interface is a String-typed
 * {@link Callable} interface with an additional method,
 * {@link #callPrimitive()}, to call upon primitive boolean
 * types and return their results as Strings.
 */
@FunctionalInterface
public interface BooleanCallable extends Callable<String> {

    @Override
    default String call() throws Exception {
        return callPrimitive() ? "true" : "false";
    }

    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    boolean callPrimitive() throws Exception;

}
