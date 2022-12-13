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
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmithyBuilderTest {
    @Test
    public void includesCallingClassName() {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> new Foo().test());

        assertThat(e.getMessage(), equalTo("foo was not set on the builder (builder class is probably "
                                           + "software.amazon.smithy.utils.SmithyBuilderTest$Foo)"));
    }

    private static final class Foo {
        public void test() {
            SmithyBuilder.requiredState("foo", null);
        }
    }
}
