/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ReadonlyTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;
import software.amazon.smithy.utils.MapUtils;

public class IdentifierBindingIndexTest {

    @Test
    public void returnsEmptyMapForUnknownBindings() {
        Model model = Model.builder().build();
        IdentifierBindingIndex index = IdentifierBindingIndex.of(model);

        assertThat(index.getOperationBindingType(ShapeId.from("ns.foo#A"), ShapeId.from("ns.foo#B")),
                equalTo(IdentifierBindingIndex.BindingType.NONE));
        assertThat(index.getOperationInputBindings(ShapeId.from("ns.foo#A"), ShapeId.from("ns.foo#B")),
                equalTo(Collections.emptyMap()));
    }

    @Test
    public void findsImplicitInstanceBindings() {
        StringShape id = StringShape.builder().id("ns.foo#Id").build();
        StructureShape input = StructureShape.builder()
                .id("ns.foo#Input")
                .addMember(MemberShape.builder()
                        .id("ns.foo#Input$abc")
                        .addTrait(new RequiredTrait())
                        .target("ns.foo#Id")
                        .build())
                .build();
        OperationShape operation = OperationShape.builder().id("ns.foo#Operation").input(input.getId()).build();
        ResourceShape resource = ResourceShape.builder()
                .id("ns.foo#Resource")
                .addIdentifier("abc", "ns.foo#Id")
                .addOperation(operation.getId())
                .build();
        Model model = Model.assembler().addShapes(id, resource, operation, input).assemble().unwrap();
        IdentifierBindingIndex index = IdentifierBindingIndex.of(model);

        assertThat(index.getOperationBindingType(resource.getId(), operation.getId()),
                equalTo(IdentifierBindingIndex.BindingType.INSTANCE));

        Map<String, String> expectedBindings = new HashMap<>();
        expectedBindings.put("abc", "abc");
        assertThat(index.getOperationInputBindings(resource.getId(), operation.getId()), equalTo(expectedBindings));
    }

    @Test
    public void findsCollectionBindings() {
        StringShape id = StringShape.builder().id("ns.foo#Id").build();
        StructureShape input = StructureShape.builder().id("ns.foo#Input").build();
        OperationShape operation = OperationShape.builder()
                .id("ns.foo#Operation")
                .input(input.getId())
                .build();
        OperationShape listOperation = OperationShape.builder()
                .id("ns.foo#ListResources")
                .addTrait(new ReadonlyTrait())
                .input(input.getId())
                .build();
        OperationShape createOperation = OperationShape.builder()
                .id("ns.foo#CreateResource")
                .input(input.getId())
                .build();
        ResourceShape resource = ResourceShape.builder()
                .id("ns.foo#Resource")
                .addIdentifier("abc", "ns.foo#Id")
                .create(createOperation.getId())
                .list(listOperation.getId())
                .addCollectionOperation(operation.getId())
                .build();
        Model model = Model.assembler()
                .addShapes(id, resource, operation, input, listOperation, createOperation)
                .assemble()
                .unwrap();
        IdentifierBindingIndex index = IdentifierBindingIndex.of(model);

        assertThat(index.getOperationBindingType(resource.getId(), operation.getId()),
                equalTo(IdentifierBindingIndex.BindingType.COLLECTION));
        assertThat(index.getOperationInputBindings(resource.getId(), operation.getId()),
                equalTo(Collections.emptyMap()));
        assertThat(index.getOperationBindingType(resource.getId(), listOperation.getId()),
                equalTo(IdentifierBindingIndex.BindingType.COLLECTION));
        assertThat(index.getOperationInputBindings(
                resource.getId(),
                listOperation.getId()), equalTo(Collections.emptyMap()));
        assertThat(index.getOperationBindingType(resource.getId(), createOperation.getId()),
                equalTo(IdentifierBindingIndex.BindingType.COLLECTION));
        assertThat(index.getOperationInputBindings(
                resource.getId(),
                createOperation.getId()), equalTo(Collections.emptyMap()));
    }

    @Test
    public void findsExplicitBindings() {
        Map<String, String> expectedBindings = new HashMap<>();
        expectedBindings.put("abc", "def");
        StructureShape input = StructureShape.builder()
                .id("ns.foo#Input")
                .addMember(MemberShape.builder()
                        .id("ns.foo#Input$def")
                        .addTrait(new RequiredTrait())
                        .addTrait(new ResourceIdentifierTrait("abc", SourceLocation.NONE))
                        .target("smithy.api#String")
                        .build())
                .build();
        OperationShape operation = OperationShape.builder().id("ns.foo#Operation").input(input.getId()).build();
        ResourceShape resource = ResourceShape.builder()
                .id("ns.foo#Resource")
                .addIdentifier("abc", "smithy.api#String")
                .addOperation(operation.getId())
                .build();
        Model model = Model.assembler().addShapes(resource, operation, input).assemble().unwrap();
        IdentifierBindingIndex index = IdentifierBindingIndex.of(model);

        assertThat(index.getOperationBindingType(resource.getId(), operation.getId()),
                equalTo(IdentifierBindingIndex.BindingType.INSTANCE));
        assertThat(index.getOperationInputBindings(resource.getId(), operation.getId()), equalTo(expectedBindings));
    }

    @Test
    public void explicitBindingsPreferred() {
        // Ensure that this does not fail to load. This previously failed when using Collectors.toMap due to
        // a collision in the keys used to map an identifier to multiple members.
        Model model = Model.assembler()
                .addImport(getClass().getResource("colliding-resource-identifiers.smithy"))
                .assemble()
                .unwrap();
        IdentifierBindingIndex index = IdentifierBindingIndex.of(model);

        // Ensure that the explicit trait bindings are preferred over implicit bindings.
        assertThat(index.getOperationInputBindings(
                ShapeId.from("smithy.example#Foo"),
                ShapeId.from("smithy.example#GetFoo")),
                equalTo(MapUtils.of("bar", "bam")));
    }
}
