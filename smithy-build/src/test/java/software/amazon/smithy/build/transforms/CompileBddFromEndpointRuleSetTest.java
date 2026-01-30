/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;

public class CompileBddFromEndpointRuleSetTest {

    @Test
    public void compilesAndAttachesBddTrait() throws Exception {
        ShapeId serviceId = ShapeId.from("smithy.example#ExampleService");
        Model model = Model.assembler()
                .discoverModels()
                .addImport(Paths.get(getClass().getResource("compile-bdd-from-endpoint-ruleset.smithy").toURI()))
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .build();
        Model result = new CompileBddFromEndpointRuleSet().transform(context);
        Shape serviceShape = result.expectShape(serviceId);
        assertTrue(serviceShape.hasTrait(EndpointBddTrait.ID));
    }
}
