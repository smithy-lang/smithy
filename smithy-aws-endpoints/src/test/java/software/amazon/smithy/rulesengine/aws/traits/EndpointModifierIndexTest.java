/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.traits;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;

class EndpointModifierIndexTest {
    @Test
    public void loadsFromModel() {
        final Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("endpointModifierIndex.smithy"))
                .assemble()
                .unwrap();

        ShapeId service1 = getServiceShapeId(model, "ns.foo#Service1");
        ShapeId service2 = getServiceShapeId(model, "ns.foo#Service2");
        ShapeId service3 = getServiceShapeId(model, "ns.foo#Service3");
        ShapeId service4 = getServiceShapeId(model, "ns.foo#Service4");

        EndpointModifierIndex index = new EndpointModifierIndex(model);

        assertEquals(index.getEndpointModifierTraits(service1).size(), 1);
        // Assert this works if you pass in the shape as well.
        assertEquals(index.getEndpointModifierTraits(model.expectShape(service1)).size(), 1);
        assertInstanceOf(StandardRegionalEndpointsTrait.class,
                index.getEndpointModifierTraits(service1).get(StandardRegionalEndpointsTrait.ID));

        assertEquals(index.getEndpointModifierTraits(service2).size(), 1);
        assertInstanceOf(StandardPartitionalEndpointsTrait.class,
                index.getEndpointModifierTraits(service2).get(StandardPartitionalEndpointsTrait.ID));

        assertEquals(index.getEndpointModifierTraits(service3).size(), 2);
        assertInstanceOf(StandardRegionalEndpointsTrait.class,
                index.getEndpointModifierTraits(service3).get(StandardRegionalEndpointsTrait.ID));
        assertInstanceOf(DualStackOnlyEndpointsTrait.class,
                index.getEndpointModifierTraits(service3).get(DualStackOnlyEndpointsTrait.ID));

        assertEquals(index.getEndpointModifierTraits(service4).size(), 2);
        assertInstanceOf(StandardPartitionalEndpointsTrait.class,
                index.getEndpointModifierTraits(service4).get(StandardPartitionalEndpointsTrait.ID));
        assertInstanceOf(RuleBasedEndpointsTrait.class,
                index.getEndpointModifierTraits(service4).get(RuleBasedEndpointsTrait.ID));
    }

    @Test
    public void indexSkipsLoadingTraitsWhenDefinitionIsMissing() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("endpointModifierIndex.smithy"))
                .assemble()
                .unwrap();

        // Remove trait shape definition from model to verify index can skip it
        model = model.toBuilder().removeShape(StandardRegionalEndpointsTrait.ID).build();

        EndpointModifierIndex index = new EndpointModifierIndex(model);

        ShapeId service1 = getServiceShapeId(model, "ns.foo#Service1");
        ShapeId service3 = getServiceShapeId(model, "ns.foo#Service3");

        assertEquals(index.getEndpointModifierTraits(service1).size(), 0);
        assertEquals(index.getEndpointModifierTraits(service3).size(), 1);
    }

    private ShapeId getServiceShapeId(Model model, String service) {
        return model
                .expectShape(ShapeId.from(service), ServiceShape.class)
                .toShapeId();
    }
}
