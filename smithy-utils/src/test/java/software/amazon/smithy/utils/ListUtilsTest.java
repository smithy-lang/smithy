/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class ListUtilsTest {
    @Test
    public void copyOfEmptyIsEmpty() {
        assertThat(ListUtils.copyOf(Collections.emptyList()), empty());
    }

    @Test
    public void copyOfIsSame() {
        assertThat(ListUtils.copyOf(Collections.singletonList("Jason")), contains("Jason"));
    }

    @Test
    public void ofEmptyIsEmpty() {
        assertThat(ListUtils.of(), empty());
    }

    @Test
    public void ofOneIsOne() {
        assertThat(ListUtils.of("Jason"), contains("Jason"));
    }

    @Test
    public void ofManyIsMany() {
        assertThat(ListUtils.of("Jason", "Michael", "Kevin"), containsInAnyOrder("Jason", "Michael", "Kevin"));
    }

    @Test
    public void collectsToList() {
        assertThat(Stream.of("Jason", "Michael", "Kevin").collect(ListUtils.toUnmodifiableList()),
                containsInAnyOrder("Jason", "Michael", "Kevin"));
    }
}
