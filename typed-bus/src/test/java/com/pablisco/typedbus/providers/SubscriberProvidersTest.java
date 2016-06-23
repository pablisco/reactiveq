package com.pablisco.typedbus.providers;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

public class SubscriberProvidersTest {

    @Mock
    private Iterable<String> mockIterable;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void createsIterableProvider() throws Exception {
        final SubscriberProvider iterableProvider = SubscriberProviders.providerFrom(mockIterable);
        assertThat(iterableProvider).isInstanceOf(SubscriberProvider.class);
    }

}