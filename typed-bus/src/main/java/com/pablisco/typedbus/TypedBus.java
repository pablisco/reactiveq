package com.pablisco.typedbus;

public interface TypedBus<T> {

    void messageAll(TypedBusMessage<T> message);

}
