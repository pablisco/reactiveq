package com.pablisco.typedbus;

public interface TypedBusMessage<T> {

    void apply(T receiver);

}
