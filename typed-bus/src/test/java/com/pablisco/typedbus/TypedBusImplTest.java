package com.pablisco.typedbus;

import com.pablisco.typedbus.predicates.ReceiverPredicate;
import com.pablisco.typedbus.providers.SubscriberProvider;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static com.pablisco.typedbus.predicates.PredicateMatchers.isAnyPredicate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class TypedBusImplTest {

    public static class TestType {

    }

    private TypedBusImpl<TestType> subject;

    @Mock
    private SubscriberProvider<TestType> mockSubscriberProvider;

    @Mock
    private TypedBusMessage<TestType> mockMessage;

    @Captor
    private ArgumentCaptor<TestType> captorTestType;

    private List<TestType> testItems;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        testItems = Arrays.asList(new TestType(), new TestType(), new TestType());

        Mockito.when(mockSubscriberProvider.findReceivers(isAnyPredicate(TestType.class)))
                .thenReturn(testItems);

        subject = new TypedBusImpl<>(mockSubscriberProvider);
    }

    @Test
    public void canCreateTypedBus() throws Exception {
        assertThat(subject).isInstanceOf(TypedBus.class);
    }

    @Test
    public void canSendMessageToAllReceivers() throws Exception {
        subject.messageAll(mockMessage);
        verify(mockMessage, times(testItems.size())).apply(captorTestType.capture());
        assertThat(captorTestType.getAllValues()).containsAll(testItems);
    }

    private <T> ReceiverPredicate<T> anyReceiverPredicate(Class<T> type) {
        return any();
    }

}