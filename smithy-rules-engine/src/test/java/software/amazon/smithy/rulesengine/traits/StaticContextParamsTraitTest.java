/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;

public class StaticContextParamsTraitTest {

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();

        OperationShape operationShape = result.expectShape(ShapeId.from("smithy.example#GetThing"),
                OperationShape.class);

        StaticContextParamsTrait trait = operationShape.getTrait(StaticContextParamsTrait.class).get();
        assertEquals(trait.getParameters(),
                MapUtils.of(
                        "stringBar",
                        StaticContextParamDefinition.builder()
                                .value(StringNode.from("some value"))
                                .build(),
                        "boolBar",
                        StaticContextParamDefinition.builder()
                                .value(Node.from(true))
                                .build()));
    }
}
