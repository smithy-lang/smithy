/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.BooleanShape;

public class GenerateEnumDirectiveTest {
    @Test
    public void validatesShapeType() {
        BooleanShape shape = BooleanShape.builder().id("smithy.example#Foo").build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new GenerateEnumDirective<TestContext, TestSettings>(null, null, shape);
        });
    }
}
