/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
