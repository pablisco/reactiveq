package com.pablisco.typedbus.predicates;

import org.mockito.Matchers;

public class PredicateMatchers {

    @SuppressWarnings("unchecked")
    public static <T> ReceiverPredicate<T> isAnyPredicate(Class<T> type) {
        return Matchers.isA(AnyReceiverPredicate.class);
    }

}
