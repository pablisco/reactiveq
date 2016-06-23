package com.pablisco.typedbus.predicates;

class AnyReceiverPredicate<T> implements ReceiverPredicate<T> {

    @Override
    public boolean apply(T t) {
        return true;
    }

}
