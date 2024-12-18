/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.transforms;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class IncludeTraitsTest {

    @Test
    public void removesTraitsNotInList() {
        StringShape stringShape = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(new SensitiveTrait())
                .addTrait(new DocumentationTrait("docs"))
                .build();
        Model model = Model.assembler()
                .addShape(stringShape)
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("traits", Node.fromStrings("documentation")))
                .build();
        Model result = new IncludeTraits().transform(context);

        assertThat(result.expectShape(ShapeId.from("ns.foo#baz")).getTrait(DocumentationTrait.class),
                not(Optional.empty()));
        assertThat(result.expectShape(ShapeId.from("ns.foo#baz")).getTrait(SensitiveTrait.class),
                is(Optional.empty()));

        assertTrue(result.getTraitDefinition(ShapeId.from("smithy.api#documentation")).isPresent());
        assertFalse(result.getTraitDefinition(ShapeId.from("smithy.api#sensitive")).isPresent());
    }

    @Test
    public void includesBuiltinTraits() {
        StringShape stringShape = StringShape.builder()
                .id("ns.foo#baz")
                .addTrait(new SensitiveTrait())
                .addTrait(new DocumentationTrait("docs", SourceLocation.NONE))
                .build();
        Model model = Model.assembler()
                .addShape(stringShape)
                .assemble()
                .unwrap();
        TransformContext context = TransformContext.builder()
                .model(model)
                .settings(Node.objectNode().withMember("traits", Node.fromStrings("smithy.api#")))
                .build();
        Model result = new IncludeTraits().transform(context);

        assertThat(result.expectShape(ShapeId.from("ns.foo#baz")).getTrait(DocumentationTrait.class),
                not(Optional.empty()));
        assertThat(result.expectShape(ShapeId.from("ns.foo#baz")).getTrait(SensitiveTrait.class),
                not(Optional.empty()));
        assertTrue(result.getTraitDefinition(ShapeId.from("smithy.api#documentation")).isPresent());
        assertTrue(result.getTraitDefinition(ShapeId.from("smithy.api#sensitive")).isPresent());
    }
}
