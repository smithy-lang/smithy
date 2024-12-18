/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.SourceLocation;

public class DefaultNodeWriterTest {
    @ParameterizedTest
    @MethodSource("data")
    public void testWriter(Node node, String expected) {
        String result = Node.prettyPrintJson(node);
        assertThat(result, equalTo(expected));
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                {Node.from(true), "true"},
                {Node.from("foo"), "\"foo\""},
                {Node.from(10), "10"},
                {Node.from(10.0), "10.0"},
                {Node.parse("10.2"), "10.2"},
                {new NullNode(SourceLocation.none()), "null"},
                {new ArrayNode(
                        Arrays.asList(Node.from(1), Node.from(2)),
                        SourceLocation.none()), String.format("[%n    1,%n    2%n]")},
                {Node.objectNode()
                        .withMember("foo", Node.from("foo"))
                        .withMember("baz", Node.from(1))
                        .withMember("bar",
                                Node.objectNode()
                                        .withMember("qux",
                                                Node.arrayNode()
                                                        .withValue(Node.from("ipsum")))),
                        String.format("{%n"
                                + "    \"foo\": \"foo\",%n"
                                + "    \"baz\": 1,%n"
                                + "    \"bar\": {%n"
                                + "        \"qux\": [%n"
                                + "            \"ipsum\"%n"
                                + "        ]%n"
                                + "    }%n"
                                + "}")
                },
                {Node.objectNode()
                        .withMember("foo",
                                Node.objectNode()
                                        .withMember("bar",
                                                Node.arrayNode()
                                                        .withValue(Node.from("baz"))))
                        .withMember("bam",
                                Node.arrayNode()
                                        .withValue(Node.objectNode()
                                                .withMember("abc", Node.from(123)))),
                        String.format("{%n"
                                + "    \"foo\": {%n"
                                + "        \"bar\": [%n"
                                + "            \"baz\"%n"
                                + "        ]%n"
                                + "    },%n"
                                + "    \"bam\": [%n"
                                + "        {%n"
                                + "            \"abc\": 123%n"
                                + "        }%n"
                                + "    ]%n"
                                + "}")
                }
        });
    }
}
