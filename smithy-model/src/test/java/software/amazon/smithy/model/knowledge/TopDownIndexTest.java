/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class TopDownIndexTest {
    @Test
    public void findDirectChildren() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Service")
                .version("1")
                .addResource("ns.foo#Resource")
                .build();
        ResourceShape resource = ResourceShape.builder().id("ns.foo#Resource").build();
        Model model = Model.builder().addShapes(service, resource).build();
        TopDownIndex childIndex = TopDownIndex.of(model);

        assertThat(childIndex.getContainedOperations(service), empty());
        assertThat(childIndex.getContainedResources(service), contains(resource));
        assertThat(childIndex.getContainedOperations(resource), empty());
        assertThat(childIndex.getContainedResources(resource), empty());
    }

    @Test
    public void findsAllChildren() {
        ServiceShape service = ServiceShape.builder()
                .id("ns.foo#Service")
                .version("1")
                .addResource("ns.foo#A")
                .build();
        ResourceShape resourceA = ResourceShape.builder()
                .id("ns.foo#A")
                .list(ShapeId.from("ns.foo#List"))
                .addResource("ns.foo#B")
                .build();

        ResourceShape resourceB = ResourceShape.builder().id("ns.foo#B").addOperation("ns.foo#Operation").build();
        OperationShape operation = OperationShape.builder().id("ns.foo#Operation").build();
        OperationShape list = OperationShape.builder().id("ns.foo#List").build();
        Model model = Model.builder().addShapes(service, resourceA, resourceB, operation, list).build();
        TopDownIndex childIndex = TopDownIndex.of(model);

        assertThat(childIndex.getContainedResources(service.getId()), containsInAnyOrder(resourceA, resourceB));
        assertThat(childIndex.getContainedOperations(service.getId()), containsInAnyOrder(operation, list));

        assertThat(childIndex.getContainedResources(resourceA.getId()), contains(resourceB));
        assertThat(childIndex.getContainedOperations(resourceA.getId()), containsInAnyOrder(operation, list));

        assertThat(childIndex.getContainedResources(resourceB.getId()), empty());
        assertThat(childIndex.getContainedOperations(resourceB.getId()), contains(operation));

        assertThat(childIndex.getContainedOperations(operation.getId()), empty());
        assertThat(childIndex.getContainedResources(operation.getId()), empty());

        assertThat(childIndex.getContainedResources(ShapeId.from("ns.foo#NotThere")), empty());
    }

    @Test
    public void preservesModeledOrder() {
        Model model = Model.assembler()
                .addImport(TopDownIndexTest.class.getResource("top-down-order.smithy"))
                .assemble()
                .unwrap();

        TopDownIndex index = TopDownIndex.of(model);
        List<ShapeId> serviceOperations = index
                .getContainedOperations(ShapeId.from("com.example#Service"))
                .stream()
                .map(Shape::toShapeId)
                .collect(Collectors.toList());
        assertThat(serviceOperations,
                contains(
                        ShapeId.from("com.example#OperationB"),
                        ShapeId.from("com.example#OperationA"),
                        ShapeId.from("com.example#OperationC"),
                        ShapeId.from("com.example#OperationD"),
                        ShapeId.from("com.example#OperationO"),
                        ShapeId.from("com.example#OperationG")));

        List<ShapeId> resourceOperations = index
                .getContainedOperations(ShapeId.from("com.example#ResourceA"))
                .stream()
                .map(Shape::toShapeId)
                .collect(Collectors.toList());
        assertThat(resourceOperations,
                contains(
                        ShapeId.from("com.example#OperationD"),
                        ShapeId.from("com.example#OperationO"),
                        ShapeId.from("com.example#OperationG")));

        List<ShapeId> serviceResources = index
                .getContainedResources(ShapeId.from("com.example#Service"))
                .stream()
                .map(Shape::toShapeId)
                .collect(Collectors.toList());
        assertThat(serviceResources,
                contains(
                        ShapeId.from("com.example#ResourceA"),
                        ShapeId.from("com.example#ResourceC"),
                        ShapeId.from("com.example#ResourceB")));

        List<ShapeId> resourceResources = index
                .getContainedResources(ShapeId.from("com.example#ResourceA"))
                .stream()
                .map(Shape::toShapeId)
                .collect(Collectors.toList());
        assertThat(resourceResources,
                contains(
                        ShapeId.from("com.example#ResourceC"),
                        ShapeId.from("com.example#ResourceB")));
    }
}
