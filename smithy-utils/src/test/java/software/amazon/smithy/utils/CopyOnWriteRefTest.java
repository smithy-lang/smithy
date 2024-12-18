/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class CopyOnWriteRefTest {
    @Test
    public void doesNotCopyOwnedValues() {
        Map<String, String> map = new HashMap<>();
        CopyOnWriteRef<Map<String, String>> ref = CopyOnWriteRef.fromOwned(map);

        assertThat(ref.peek(), is(map));
        assertThat(ref.get(), is(map));
    }

    @Test
    public void copiesBorrowedValues() {
        Map<String, String> map = new HashMap<>();
        CopyOnWriteRef<Map<String, String>> ref = CopyOnWriteRef.fromBorrowed(map, HashMap::new);

        assertThat(ref.peek(), sameInstance(map));
        assertThat(ref.get(), not(sameInstance(map)));

        // Now that a copy is made, it's returned from peek.
        assertThat(ref.peek(), not(sameInstance(map)));

        ref.get().put("foo", "hi");

        assertThat(map.entrySet(), empty());
        assertThat(ref.get(), hasKey("foo"));
        assertThat(ref.peek(), hasKey("foo"));
    }
}
