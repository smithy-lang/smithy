/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.utils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmithyBuilderTest {
    @Test
    public void includesCallingClassName() {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> new Foo().test());

        assertThat(e.getMessage(),
                equalTo("foo was not set on the builder (builder class is probably "
                        + "software.amazon.smithy.utils.SmithyBuilderTest$Foo)"));
    }

    private static final class Foo {
        public void test() {
            SmithyBuilder.requiredState("foo", null);
        }
    }
}
