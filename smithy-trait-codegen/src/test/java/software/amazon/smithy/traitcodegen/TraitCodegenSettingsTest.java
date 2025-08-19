/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TraitCodegenSettingsTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "smithy",
            "smithy.api",
            "smithy.waiters",
            "smithy.test",
            "smithy.api.foo",
            "smithy.waiters.bar",
            "smithy.test.baz",
            "Smithy.api",
            "sMithy",
            "smithy.API",
            "smiThy.Api.Baz"
    })
    void constructorRejectsReservedNamespaces(String namespace) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new TraitCodegenSettings("com.example",
                        namespace,
                        Collections.emptyList(),
                        Collections.emptyList()));
        assertEquals("The `smithy` namespace and its sub-namespaces are reserved.", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "smithy.foo",
            "smithy.bar.baz",
            "example.namespace",
            "my.custom.namespace",
            "smithy.custom",
            "smithyapi",
            "smithy.apiextension"
    })
    void constructorAcceptsValidNamespaces(String namespace) {
        assertDoesNotThrow(() -> new TraitCodegenSettings(
                "com.example",
                namespace,
                Collections.emptyList(),
                Collections.emptyList()));
    }

    @ParameterizedTest
    @MethodSource("nullParameterCombinations")
    void constructorRequiresNonNullParameters(String packageName, String namespace) {
        assertThrows(NullPointerException.class,
                () -> new TraitCodegenSettings(packageName,
                        namespace,
                        Collections.emptyList(),
                        Collections.emptyList()));
    }

    static Stream<Arguments> nullParameterCombinations() {
        return Stream.of(
                Arguments.of(null, "smithy.foo"),
                Arguments.of("com.example", null));
    }

    @Test
    void gettersReturnCorrectValues() {
        TraitCodegenSettings settings = new TraitCodegenSettings(
                "com.example",
                "smithy.foo",
                Collections.emptyList(),
                Collections.emptyList());

        assertEquals("com.example", settings.packageName());
        assertEquals("smithy.foo", settings.smithyNamespace());
        assertEquals(Collections.emptyList(), settings.headerLines());
        assertEquals(Collections.emptyList(), settings.excludeTags());
    }
}
