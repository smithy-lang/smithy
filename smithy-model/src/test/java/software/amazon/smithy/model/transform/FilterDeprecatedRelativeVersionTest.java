/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.utils.ListUtils;

public class FilterDeprecatedRelativeVersionTest {
    static List<Arguments> semverSupplier() {
        return ListUtils.of(
                Arguments.of("1.0.0", "1.0.0", 0),
                Arguments.of("1.0.0", "1.0.1", -1),
                Arguments.of("1.1.0", "1.1.1", -1),
                Arguments.of("1.1.0", "1.0.1", 1),
                Arguments.of("1.1.1", "1.1.1.1", -1),
                Arguments.of("1.0.0.1", "1.0.0", 1),
                Arguments.of("1.0.0", "1.0", 0),
                Arguments.of("20.20.0.1", "20.20.1.0", -1),
                Arguments.of("20.20.1.0", "20.20.1.0-PATCH", -1));
    }

    @ParameterizedTest
    @MethodSource("semverSupplier")
    void testSemverComparison(String semver1, String semver2, int expected) {
        int result = FilterDeprecatedRelativeVersion.compareSemVer(semver1, semver2);
        switch (expected) {
            case 0:
                assertEquals(result, 0);
                break;
            case -1:
                assertTrue(result < 0);
                break;
            case 1:
                assertTrue(result > 0);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + expected);
        }
    }

    public static List<String> fileSource() {
        return ListUtils.of("deprecated-shapes", "deprecated-members", "deprecated-traits");
    }

    @ParameterizedTest
    @MethodSource("fileSource")
    void compareTransform(String prefix) {
        Model before = Model.assembler()
                .addImport(FilterDeprecatedRelativeDate.class
                        .getResource("deprecated-version/" + prefix + "-before.smithy"))
                .assemble()
                .unwrap();
        Model actualResult = ModelTransformer.create().filterDeprecatedRelativeVersion(before, "1.1.0");
        Model expectedResult = Model.assembler()
                .addImport(FilterDeprecatedRelativeDate.class
                        .getResource("deprecated-version/" + prefix + "-after.smithy"))
                .assemble()
                .unwrap();

        Node actualNode = ModelSerializer.builder().build().serialize(actualResult);
        Node expectedNode = ModelSerializer.builder().build().serialize(expectedResult);
        Node.assertEquals(actualNode, expectedNode);
    }
}
