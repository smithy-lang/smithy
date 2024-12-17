/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ClientDiscoveredEndpointTraitTest {

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        OperationShape operation = result
                .expectShape(ShapeId.from("ns.foo#GetObject"))
                .asOperationShape()
                .get();
        ClientDiscoveredEndpointTrait trait = operation.getTrait(ClientDiscoveredEndpointTrait.class).get();

        assertTrue(trait.isRequired());
    }
}
