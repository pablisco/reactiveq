package com.pablisco.typedbus;

public interface Broker {

    <T> void messageFirst();

    <T> void messageAll();

}
