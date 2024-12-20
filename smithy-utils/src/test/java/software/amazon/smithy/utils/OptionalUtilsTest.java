/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
