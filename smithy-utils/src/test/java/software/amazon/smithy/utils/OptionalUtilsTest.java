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
import static org.hamcrest.Matchers.equalTo;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class OptionalUtilsTest {
    @Test
    public void orUsesValue() {
        assertThat(OptionalUtils.or(Optional.of("Kevin"), () -> Optional.of("foo")).get(), equalTo("Kevin"));
    }

    @Test
    public void orGetSupplierValue() {
        assertThat(OptionalUtils.or(Optional.empty(), () -> Optional.of("foo")).get(), equalTo("foo"));
    }

    @Test
    public void streamUsesValue() {
        assertThat(OptionalUtils.stream(Optional.of("Kevin")).count(), equalTo(1L));
    }

    @Test
    public void streamIsEmpty() {
        assertThat(OptionalUtils.stream(Optional.empty()).count(), equalTo(0L));
    }

    @Test
    public void invokesPresentAction() {
        OptionalUtils.ifPresentOrElse(Optional.of("Kevin"),
                (i) -> {},
                () -> new RuntimeException("Failure"));
    }

    @Test
    public void invokesEmptyAction() {
        OptionalUtils.ifPresentOrElse(Optional.empty(),
                (i) -> new RuntimeException("Failure"),
                () -> {});
    }
}
