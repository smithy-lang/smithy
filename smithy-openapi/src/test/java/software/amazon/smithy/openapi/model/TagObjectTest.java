/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.openapi.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TagObjectTest {
    private static final TagObject TAG_OBJECT_1 =
            TagObject.builder()
                    .name("tag1")
                    .description("description1")
                    .externalDocs(ExternalDocumentation.builder().url("url1").build())
                    .build();

    private static Stream<Arguments> testData() {
        return Stream.of(
                Arguments.of("tag1", "description1", ExternalDocumentation.builder().url("url1").build(), 0),
                Arguments.of("tag0", "description1", ExternalDocumentation.builder().url("url1").build(), 1),
                Arguments.of("tag1", "description1", ExternalDocumentation.builder().url("url2").build(), -1),
                Arguments.of("tag2", "description1", ExternalDocumentation.builder().url("url1").build(), -1),
                Arguments.of("tag1", "description2", ExternalDocumentation.builder().url("url1").build(), -1),
                Arguments.of("tag1", null, ExternalDocumentation.builder().url("url1").build(), 1),
                Arguments.of("tag1", "description1", null, 1));
    }

    @ParameterizedTest
    @MethodSource("testData")
    void testCompareTo(String name, String description, ExternalDocumentation doc, int expected) {
        TagObject tagObject2 = TagObject.builder().name(name).description(description).externalDocs(doc).build();
        assertThat(TAG_OBJECT_1.compareTo(tagObject2), equalTo(expected));
    }
}
