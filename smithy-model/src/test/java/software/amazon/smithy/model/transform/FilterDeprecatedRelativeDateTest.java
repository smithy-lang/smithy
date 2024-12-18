/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ModelSerializer;
import software.amazon.smithy.utils.ListUtils;

public class FilterDeprecatedRelativeDateTest {

    public static List<String> fileSource() {
        return ListUtils.of("deprecated-shapes", "deprecated-members", "deprecated-traits");
    }

    @ParameterizedTest
    @MethodSource("fileSource")
    void compareTransform(String prefix) {
        Model before = Model.assembler()
                .addImport(
                        FilterDeprecatedRelativeDate.class.getResource("deprecated-date/" + prefix + "-before.smithy"))
                .assemble()
                .unwrap();
        Model actualResult = ModelTransformer.create().filterDeprecatedRelativeDate(before, "2024-10-10");
        Model expectedResult = Model.assembler()
                .addImport(
                        FilterDeprecatedRelativeDate.class.getResource("deprecated-date/" + prefix + "-after.smithy"))
                .assemble()
                .unwrap();

        Node actualNode = ModelSerializer.builder().build().serialize(actualResult);
        Node expectedNode = ModelSerializer.builder().build().serialize(expectedResult);
        Node.assertEquals(actualNode, expectedNode);
    }
}
