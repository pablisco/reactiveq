package com.pablisco.typedbus;

import com.pablisco.typedbus.predicates.ReceiverPredicates;
import com.pablisco.typedbus.providers.SubscriberProvider;

class TypedBusImpl<T> implements TypedBus<T> {

    private final SubscriberProvider<T> subscriberProvider;

    public TypedBusImpl(SubscriberProvider<T> subscriberProvider) {
        this.subscriberProvider = subscriberProvider;
    }

    @Override
    public void messageAll(TypedBusMessage<T> message) {
        for (T item : subscriberProvider.findReceivers(ReceiverPredicates.<T>any())) {
            message.apply(item);
        }
    }

}
