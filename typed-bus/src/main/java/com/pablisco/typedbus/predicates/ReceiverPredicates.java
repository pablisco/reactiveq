package com.pablisco.typedbus.predicates;

public class ReceiverPredicates {

    @SuppressWarnings("unchecked")
    public static <T> ReceiverPredicate<T> any() {
        return (ReceiverPredicate<T>) new AnyReceiverPredicate<>();
    }

}
