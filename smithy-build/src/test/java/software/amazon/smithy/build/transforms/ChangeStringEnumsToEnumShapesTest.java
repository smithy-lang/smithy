/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;

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
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;

public class ChangeStringEnumsToEnumShapesTest {
    @Test
    public void canFindEnumsToConvert() {
        EnumTrait compatibleTrait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .name("foo")
                        .value("bar")
                        .build())
                .build();
        ShapeId compatibleStringId = ShapeId.fromParts("ns.foo", "CompatibleString");
        StringShape compatibleString = StringShape.builder()
                .id(compatibleStringId)
                .addTrait(compatibleTrait)
                .build();

        // This won't have a name synthesized because that setting is disabled
        EnumTrait incompatibleTrait = EnumTrait.builder()
                .addEnum(EnumDefinition.builder()
                        .value("bar")
                        .build())
                .build();
        ShapeId incompatibleStringId = ShapeId.fromParts("ns.foo", "IncompatibleString");
        StringShape incompatibleString = StringShape.builder()
                .id(incompatibleStringId)
                .addTrait(incompatibleTrait)
                .build();

        Model model = Model.assembler()
                .addShape(compatibleString)
                .addShape(incompatibleString)
                .assemble()
                .unwrap();

        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("changeStringEnumsToEnumShapes", Node.objectNode()))
                .build();

        Model result = new ChangeStringEnumsToEnumShapes().transform(context);

        assertThat(result.expectShape(compatibleStringId).getType(), Matchers.is(ShapeType.ENUM));
        assertThat(result.expectShape(compatibleStringId).members(), Matchers.hasSize(1));
        assertThat(result.expectShape(compatibleStringId).members().iterator().next(),
                Matchers.equalTo(MemberShape.builder()
                        .id(compatibleStringId.withMember("foo"))
                        .target(UnitTypeTrait.UNIT)
                        .addTrait(EnumValueTrait.builder().stringValue("bar").build())
                        .build()));

        assertThat(result.expectShape(incompatibleStringId).getType(), Matchers.is(ShapeType.STRING));
        assertThat(result.expectShape(incompatibleStringId).members(), Matchers.hasSize(0));
    }

    @Test
    public void canSynthesizeNames() {
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

        ObjectNode config = Node.parse("{\"synthesizeNames\": true}").expectObjectNode();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(config)
                .build();

        Model result = new ChangeStringEnumsToEnumShapes().transform(context);

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
