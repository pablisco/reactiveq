package com.pablisco.typedbus;

import com.pablisco.typedbus.providers.SubscriberProvider;

public class TypedBuses {

    public static <T> TypedBus<T> typedBus(SubscriberProvider<T> subscriberProvider) {
        return new TypedBusImpl<>(subscriberProvider);
    }

}
