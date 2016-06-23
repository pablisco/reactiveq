package com.pablisco.typedbus.providers;

import com.pablisco.typedbus.predicates.ReceiverPredicate;

/**
 * Provides a way to gather objects using a {@link com.pablisco.typedbus.predicates.ReceiverPredicate}
 */
public interface SubscriberProvider<T> {

    /**
     * Used to query objects from this provider.
     * @param predicate Used to determine what types to return.
     * @return A fresh new Iterable instance with the results from the query.
     */
    Iterable<T> findReceivers(ReceiverPredicate<T> predicate);

}
