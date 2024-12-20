/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class MapTraitsTest {

    @Test
    public void doesNotReplaceUnchangedShapes() {
        ShapeId shapeId = ShapeId.from("ns.foo#id1");
        StringShape shape = StringShape.builder()
                .id(shapeId)
                .addTrait(new SensitiveTrait())
                .build();
        Model model = Model.builder().addShape(shape).build();
        ModelTransformer transformer = ModelTransformer.create();
        transformer.mapTraits(model, (s, t) -> t);
    }

    @Test
    public void appliesAllMappersToShapes() {
        ShapeId shapeId = ShapeId.from("ns.foo#id1");
        StringShape shape = StringShape.builder()
                .id(shapeId)
                .addTrait(DeprecatedTrait.builder().message("foo").build())
                .addTrait(new DocumentationTrait("docs", SourceLocation.NONE))
                .build();
        Model model = Model.builder().addShape(shape).build();
        ModelTransformer transformer = ModelTransformer.create();
        Model result = transformer.mapTraits(model,
                Arrays.asList(
                        (s, t) -> {
                            if (t instanceof DeprecatedTrait) {
                                return DeprecatedTrait.builder()
                                        .sourceLocation(t.getSourceLocation())
                                        .message("baz")
                                        .build();
                            } else {
                                return t;
                            }
                        },
                        (s, t) -> {
                            if (t instanceof DocumentationTrait) {
                                return new DocumentationTrait("changed", t.getSourceLocation());
                            } else {
                                return t;
                            }
                        }));

        assertThat(result.expectShape(shapeId).getTrait(DeprecatedTrait.class), Matchers.not(Optional.empty()));
        assertThat(result.expectShape(shapeId).getTrait(DeprecatedTrait.class).get().getMessage().get(),
                Matchers.equalTo("baz"));
        assertThat(result.expectShape(shapeId).getTrait(DocumentationTrait.class).get().getValue(),
                Matchers.equalTo("changed"));
    }
}
