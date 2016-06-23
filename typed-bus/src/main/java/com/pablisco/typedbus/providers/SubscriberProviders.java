package com.pablisco.typedbus.providers;

/**
 * Static factory for instances of {@link com.pablisco.typedbus.providers.ReceiverProvider}.
 */
public class SubscriberProviders {

    public static <T> SubscriberProvider<T> providerFrom(Iterable<T> iterable) {
        return new IterableSubscriberProvider<>(iterable);
    }

}
