package com.pablisco.typedbus.predicates;

/**
 * Used to determine if an object is valid or not.
 *
 * @param <T> The type of the object being tested.
 */
public interface ReceiverPredicate<T> {

    /**
     * Tests the provided subject.
     *
     * @return true is the subject passes this predicate or false otherwise.
     */
    boolean apply(T subject);

}
