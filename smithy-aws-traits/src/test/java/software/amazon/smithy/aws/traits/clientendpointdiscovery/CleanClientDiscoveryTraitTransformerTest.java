/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.clientendpointdiscovery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.transform.ModelTransformer;

public class CleanClientDiscoveryTraitTransformerTest {

    @Test
    public void removesTraitsWhenOperationRemoved() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();

        Model result = ModelTransformer.create().filterShapes(model, shape -> {
            return !shape.getId().toString().equals("ns.foo#DescribeEndpoints");
        });

        ServiceShape service = result
                .getShape(ShapeId.from("ns.foo#FooService"))
                .flatMap(Shape::asServiceShape)
                .get();

        OperationShape getOperation = result
                .getShape(ShapeId.from("ns.foo#GetObject"))
                .flatMap(Shape::asOperationShape)
                .get();

        OperationShape putOperation = result
                .getShape(ShapeId.from("ns.foo#PutObject"))
                .flatMap(Shape::asOperationShape)
                .get();

        MemberShape putId = result
                .getShape(ShapeId.from("ns.foo#PutObjectInput$Id"))
                .flatMap(Shape::asMemberShape)
                .get();

        assertFalse(service.hasTrait(ClientEndpointDiscoveryTrait.class));
        // discovery is required for this operation, so it keeps the trait
        assertTrue(getOperation.hasTrait(ClientDiscoveredEndpointTrait.class));
        assertFalse(putOperation.hasTrait(ClientDiscoveredEndpointTrait.class));
        assertFalse(putId.hasTrait(ClientEndpointDiscoveryIdTrait.class));
    }

    @Test
    public void doesntRemoveTraitsWhenErrorRemoved() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();

        Model result = ModelTransformer.create().filterShapes(model, shape -> {
            return !shape.getId().toString().equals("ns.foo#InvalidEndpointError");
        });

        ServiceShape service = result
                .getShape(ShapeId.from("ns.foo#FooService"))
                .flatMap(Shape::asServiceShape)
                .get();

        OperationShape getOperation = result
                .getShape(ShapeId.from("ns.foo#GetObject"))
                .flatMap(Shape::asOperationShape)
                .get();

        OperationShape putOperation = result
                .getShape(ShapeId.from("ns.foo#PutObject"))
                .flatMap(Shape::asOperationShape)
                .get();

        MemberShape putId = result
                .getShape(ShapeId.from("ns.foo#PutObjectInput$Id"))
                .flatMap(Shape::asMemberShape)
                .get();

        assertTrue(service.hasTrait(ClientEndpointDiscoveryTrait.class));
        assertTrue(getOperation.hasTrait(ClientDiscoveredEndpointTrait.class));
        assertTrue(putOperation.hasTrait(ClientDiscoveredEndpointTrait.class));
        assertTrue(putId.hasTrait(ClientEndpointDiscoveryIdTrait.class));
    }

    @Test
    public void doesntRemoveOptionalOperationTraitIfStillBoundToDiscoveryService() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("multiple-configured-services.json"))
                .assemble()
                .unwrap();

        Model result = ModelTransformer.create().filterShapes(model, shape -> {
            return !shape.getId().toString().equals("ns.foo#DescribeEndpointsFoo");
        });

        OperationShape getOperation = result
                .getShape(ShapeId.from("ns.foo#GetObjectFoo"))
                .flatMap(Shape::asOperationShape)
                .get();

        OperationShape putOperation = result
                .getShape(ShapeId.from("ns.foo#PutObject"))
                .flatMap(Shape::asOperationShape)
                .get();

        assertTrue(getOperation.hasTrait(ClientDiscoveredEndpointTrait.class));
        assertTrue(putOperation.hasTrait(ClientDiscoveredEndpointTrait.class));
    }

    @Test
    public void keepsDiscoveryIdTraitIfStillBound() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("multiple-configured-services.json"))
                .assemble()
                .unwrap();

        Model result = ModelTransformer.create().filterShapes(model, shape -> {
            return !shape.getId().toString().equals("ns.foo#DescribeEndpointsFoo");
        });

        MemberShape id = result
                .getShape(ShapeId.from("ns.foo#GetObjectInput$Id"))
                .flatMap(Shape::asMemberShape)
                .get();

        assertTrue(id.hasTrait(ClientEndpointDiscoveryIdTrait.class));

    }
}
