package com.pablisco.typedbus;

import com.pablisco.typedbus.providers.SubscriberProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class TypedBusesTest {

    @Mock
    private SubscriberProvider mockSubscriberProvider;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void canCreateBus() throws Exception {
        final TypedBus bus = TypedBuses.typedBus(mockSubscriberProvider);
        assertThat(bus).isInstanceOf(TypedBusImpl.class);
    }

}