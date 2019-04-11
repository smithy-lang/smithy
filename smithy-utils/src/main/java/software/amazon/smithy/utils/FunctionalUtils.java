package software.amazon.smithy.utils;

import java.util.function.Predicate;

/**
 * Utilities for working with functions, predicates, etc.
 */
public final class FunctionalUtils {

    private FunctionalUtils() {}

    /**
     * Negates a {@link Predicate}.
     *
     * @param predicate Predicate to negate.
     * @param <T> Value type of the predicate.
     * @return Returns a predicate that negates the given predicate.
     */
    public static <T> Predicate<T> not(Predicate<T> predicate) {
        return predicate.negate();
    }
}
