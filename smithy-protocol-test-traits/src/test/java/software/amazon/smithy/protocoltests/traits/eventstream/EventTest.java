/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits.eventstream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.NodeMapper;

public class EventTest {
    @Test
    public void testReserializeEventWithoutBytes() {
        NodeMapper mapper = new NodeMapper();
        Event expected = Event.builder()
                .type(EventType.REQUEST)
                .build();
        Event actual = mapper.deserialize(mapper.serialize(expected), Event.class);
        assertEquals(expected, actual);
    }
}
