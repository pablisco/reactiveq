package com.pablisco.typedbus;

/**
 * Static factory for instances of {@link ReceiverProvider}.
 */
public class ReceiverProviders {

    public static <T> ReceiverProvider providerFrom(Iterable<T> iterable) {
        return new IterableReceiverProvider(iterable);
    }

}
