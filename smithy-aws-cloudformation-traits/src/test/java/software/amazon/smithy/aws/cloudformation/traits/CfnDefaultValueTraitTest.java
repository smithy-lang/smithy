/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public final class CfnDefaultValueTraitTest {

    @Test
    public void detectsDefaultValue() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-service.smithy"))
                .assemble()
                .unwrap();

        assertTrue(result.getShape(ShapeId.from("smithy.example#GetFooResponse$fooId"))
                .flatMap(shape -> shape.getTrait(CfnDefaultValueTrait.class))
                .isPresent());
    }
}
