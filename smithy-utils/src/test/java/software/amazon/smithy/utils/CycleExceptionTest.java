/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CycleExceptionTest {
    @Test
    public void requiresCycles() {
        assertThrows(IllegalArgumentException.class,
                () -> new CycleException(Collections.emptyList(), Collections.emptySet()));
    }

    @Test
    public void checksNodeTypes() {
        CycleException cycleException = new CycleException(ListUtils.of("foo"), SetUtils.of("bar"));
        List<String> sortedStrings = cycleException.getSortedNodes(String.class);
        Set<String> cyclicStrings = cycleException.getCyclicNodes(String.class);

        List<Object> sortedObjects = cycleException.getSortedNodes(Object.class);
        Set<Object> cyclicObjects = cycleException.getCyclicNodes(Object.class);

        assertThrows(IllegalArgumentException.class, () -> cycleException.getSortedNodes(Integer.class));
        assertThrows(IllegalArgumentException.class, () -> cycleException.getCyclicNodes(Integer.class));
    }
}
