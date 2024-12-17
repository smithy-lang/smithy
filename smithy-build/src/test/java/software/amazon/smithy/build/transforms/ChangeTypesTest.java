/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;

public class ChangeTypesTest {
    @Test
    public void changesShapeTypes() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StructureShape b = StructureShape.builder().id("ns.foo#b").build();
        Model model = Model.assembler().addShapes(a, b).assemble().unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode()
                        .withMember("shapeTypes",
                                Node.objectNode()
                                        .withMember("ns.foo#a", "boolean")
                                        .withMember("ns.foo#b", "union")))
                .build();
        Model result = new ChangeTypes().transform(context);

        assertThat(result.expectShape(ShapeId.from("ns.foo#a")).getType(), is(ShapeType.BOOLEAN));
        assertThat(result.expectShape(ShapeId.from("ns.foo#b")).getType(), is(ShapeType.UNION));
    }

    @Test
    public void canSynthesizeEnumNames() {
        EnumTrait trait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("foo:bar")
                        .build())
                .build();
        ShapeId shapeId = ShapeId.fromParts("ns.foo", "ConvertableShape");
        StringShape initialShape = StringShape.builder()
                .id(shapeId)
                .addTrait(trait)
                .build();

        Model model = Model.assembler()
                .addShape(initialShape)
                .assemble()
                .unwrap();

        ObjectNode settings = Node.objectNode()
                .withMember("shapeTypes", Node.objectNode().withMember(shapeId.toString(), "enum"))
                .withMember("synthesizeEnumNames", true);
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(settings)
                .build();

        Model result = new ChangeTypes().transform(context);

        assertThat(result.expectShape(shapeId).getType(), Matchers.is(ShapeType.ENUM));
        assertThat(result.expectShape(shapeId).members(), Matchers.hasSize(1));
        assertThat(result.expectShape(shapeId).members().iterator().next(),
                Matchers.equalTo(MemberShape.builder()
                        .id(shapeId.withMember("foo_bar"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("foo:bar").build())
                        .build()));
    }
}
