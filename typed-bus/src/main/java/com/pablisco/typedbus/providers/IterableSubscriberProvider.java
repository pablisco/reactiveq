package com.pablisco.typedbus.providers;

class IterableSubscriberProvider<T> implements SubscriberProvider<T> {

    private final Iterable<?> iterable;

    public IterableSubscriberProvider(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public Iterable<T> findReceivers(com.pablisco.typedbus.predicates.ReceiverPredicate<T> predicate) {
        return null;
    }
}
