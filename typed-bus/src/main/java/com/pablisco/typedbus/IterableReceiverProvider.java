package com.pablisco.typedbus;

class IterableReceiverProvider implements ReceiverProvider {

    private final Iterable<?> iterable;

    public <T> IterableReceiverProvider(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public <T> Iterable<T> findReceivers(ReceiverPredicate<T> predicate) {
        return null;
    }
}
