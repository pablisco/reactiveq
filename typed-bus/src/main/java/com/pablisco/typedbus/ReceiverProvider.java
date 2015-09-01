package com.pablisco.typedbus;

/**
 * Provides a way to gather objects using a {@link ReceiverPredicate}
 */
public interface ReceiverProvider {

    /**
     * Used to query objects from this provider.
     * @param predicate Used to determine what types to return.
     * @return A fresh new Iterable instance with the results from the query.
     */
    <T> Iterable<T> findReceivers(ReceiverPredicate<T> predicate);

}
