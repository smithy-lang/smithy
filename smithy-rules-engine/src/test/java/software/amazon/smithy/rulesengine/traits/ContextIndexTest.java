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

import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

public class ContextIndexTest {
    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .discoverModels(ContextIndexTest.class.getClassLoader())
                .addImport(ContextIndexTest.class.getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void indexesClientContextParams() {
        ContextIndex index = ContextIndex.of(model);

        Map<String, ClientContextParamDefinition> clientContexts = index.getClientContextParams(
                model.expectShape(ShapeId.from("smithy.example#ExampleService"))).get().getParameters();

        assertEquals(ClientContextParamDefinition.builder()
                .type(ShapeType.STRING)
                .documentation("a client string parameter")
                .build(), clientContexts.get("stringFoo"));
        assertEquals(ClientContextParamDefinition.builder()
                .type(ShapeType.BOOLEAN)
                .documentation("a client boolean parameter")
                .build(), clientContexts.get("boolFoo"));
    }

    @Test
    public void indexesStaticContextParams() {
        ContextIndex index = ContextIndex.of(model);

        Map<String, StaticContextParamDefinition> staticContexts = index.getStaticContextParams(
                model.expectShape(ShapeId.from("smithy.example#GetThing"))).get().getParameters();

        assertEquals(StaticContextParamDefinition.builder()
                        .value(StringNode.from("some value"))
                        .build(),
                staticContexts.get("stringBar"));

        assertEquals(StaticContextParamDefinition.builder()
                        .value(BooleanNode.from(true))
                        .build(),
                staticContexts.get("boolBar"));
    }

    @Test
    public void indexesContextParam() {
        ContextIndex index = ContextIndex.of(model);

        Map<MemberShape, ContextParamTrait> contexts = index.getContextParams(
                model.expectShape(ShapeId.from("smithy.example#GetThing")));

        assertEquals(contexts.get(model.expectShape(ShapeId.from("smithy.example#GetThingInput$buzz"), MemberShape.class)),
                ContextParamTrait.builder()
                        .name("stringBaz")
                        .build());

        assertEquals(contexts.get(model.expectShape(ShapeId.from("smithy.example#GetThingInput$fuzz"), MemberShape.class)),
                ContextParamTrait.builder()
                        .name("boolBaz")
                        .build());
    }
}
