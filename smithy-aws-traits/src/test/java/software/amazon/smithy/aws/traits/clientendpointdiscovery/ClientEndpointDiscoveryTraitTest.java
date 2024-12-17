/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

public class ClientEndpointDiscoveryTraitTest {

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        ServiceShape service = result
                .expectShape(ShapeId.from("ns.foo#FooService"))
                .asServiceShape()
                .get();
        ClientEndpointDiscoveryTrait trait = service.getTrait(ClientEndpointDiscoveryTrait.class).get();

        assertEquals(trait.getOperation(), ShapeId.from("ns.foo#DescribeEndpoints"));
        assertEquals(trait.getOptionalError().get(), ShapeId.from("ns.foo#InvalidEndpointError"));
    }
}
