package com.pablisco.typedbus.predicates;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReceiverPredicatesTest {

    @Test
    public void createsAnyPredicate() throws Exception {
        ReceiverPredicate predicate = ReceiverPredicates.any();
        assertThat(predicate).isInstanceOf(AnyReceiverPredicate.class);
    }

}