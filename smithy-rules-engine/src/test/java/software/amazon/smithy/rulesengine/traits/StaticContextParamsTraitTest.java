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
        assertEquals(trait.getParameters(), MapUtils.of(
                "stringBar", StaticContextParamDefinition.builder()
                        .value(StringNode.from("some value"))
                        .build(),
                "boolBar", StaticContextParamDefinition.builder()
                        .value(Node.from(true))
                        .build()
        ));
    }
}
