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

package software.amazon.smithy.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ArgumentsTest {
    @Test
    public void throwsIfParameterNotPresent() {
        Assertions.assertThrows(CliError.class, () -> {
            Arguments arguments = new Arguments(Map.of(), List.of());
            arguments.parameter("foo");
        });
    }

    @Test
    public void throwsIfParameterIsOption() {
        Assertions.assertThrows(CliError.class, () -> {
            Arguments arguments = new Arguments(Map.of("foo", List.of()), List.of());
            arguments.parameter("foo");
        });
    }

    public void returnsDefaultParameterValue() {
        Arguments arguments = new Arguments(Map.of("foo", List.of("baz")), List.of());

        assertThat(arguments.parameter("foo", "default"), equalTo("baz"));
        assertThat(arguments.parameter("not-foo", "default"), equalTo("default"));
    }

    public void returnsDefaultRepeatedParameterValue() {
        Arguments arguments = new Arguments(Map.of("foo", List.of("a", "b")), List.of());

        assertThat(arguments.repeatedParameter("foo", List.of("default")), equalTo(List.of("a", "b")));
        assertThat(arguments.repeatedParameter("not-foo", List.of("default")), equalTo(List.of("default")));
    }

    public void hasPositionalArguments() {
        Arguments arguments = new Arguments(Map.of(), List.of("a", "b"));

        assertThat(arguments.positionalArguments(), equalTo(List.of("a", "b")));
    }
}
