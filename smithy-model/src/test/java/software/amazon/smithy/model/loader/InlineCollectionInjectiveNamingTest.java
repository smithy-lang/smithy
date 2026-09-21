/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;
import software.amazon.smithy.model.validation.ValidatedResult;

/**
 * Verifies that inline collection synthetic shapes are named injectively: two
 * inline collections whose element targets share a simple name but live in
 * different namespaces must produce two distinct synthetic shapes, each keeping
 * its own element target.
 */
public class InlineCollectionInjectiveNamingTest {

    @Test
    public void distinctTargetsWithSameSimpleNameDoNotCollide() {
        // Two files so each namespace is declared cleanly.
        Model result = Model.assembler()
                .addUnparsedModel("main.smithy",
                        "$version: \"2.1\"\n"
                                + "namespace audit\n"
                                + "structure UsesFoo {\n"
                                + "    widgets: [audit.foo#Widget]\n"
                                + "}\n"
                                + "structure UsesBar {\n"
                                + "    widgets: [audit.bar#Widget]\n"
                                + "}\n")
                .addUnparsedModel("foo.smithy",
                        "$version: \"2.1\"\nnamespace audit.foo\nstructure Widget {}\n")
                .addUnparsedModel("bar.smithy",
                        "$version: \"2.1\"\nnamespace audit.bar\nstructure Widget {}\n")
                .assemble()
                .unwrap();

        // There must be two distinct synthetic list shapes in the audit namespace.
        List<Shape> syntheticLists = result.getListShapes()
                .stream()
                .filter(shape -> shape.hasTrait(SyntheticShapeTrait.ID))
                .filter(shape -> shape.getId().getNamespace().equals("audit"))
                .collect(Collectors.toList());
        assertThat(syntheticLists, hasSize(2));

        // Each member keeps its own element target (no silent replacement).
        ShapeId fooTarget = result.expectShape(ShapeId.from("audit#UsesFoo$widgets"), MemberShape.class).getTarget();
        ShapeId barTarget = result.expectShape(ShapeId.from("audit#UsesBar$widgets"), MemberShape.class).getTarget();
        assertThat(fooTarget, is(not(barTarget)));

        ShapeId fooElement = result.expectShape(fooTarget, ListShape.class).getMember().getTarget();
        ShapeId barElement = result.expectShape(barTarget, ListShape.class).getMember().getTarget();
        assertThat(fooElement, equalTo(ShapeId.from("audit.foo#Widget")));
        assertThat(barElement, equalTo(ShapeId.from("audit.bar#Widget")));
    }

    @Test
    public void identicalTargetsShareASingleSyntheticShape() {
        Model result = Model.assembler()
                .addUnparsedModel("main.smithy",
                        "$version: \"2.1\"\n"
                                + "namespace smithy.example\n"
                                + "structure First {\n"
                                + "    a: [String]\n"
                                + "}\n"
                                + "structure Second {\n"
                                + "    b: [String]\n"
                                + "}\n")
                .assemble()
                .unwrap();

        ShapeId a = result.expectShape(ShapeId.from("smithy.example#First$a"), MemberShape.class).getTarget();
        ShapeId b = result.expectShape(ShapeId.from("smithy.example#Second$b"), MemberShape.class).getTarget();

        // Both members target the same synthetic shape, and that shape has the expected id and
        // element target. Asserting the concrete id and element guards against a regression that
        // collapsed every inline collection onto one shape.
        ShapeId expectedList = ShapeId.from("smithy.example#_SyntheticListOf__String");
        assertThat(a, equalTo(expectedList));
        assertThat(b, equalTo(expectedList));
        assertThat(result.expectShape(expectedList, ListShape.class).getMember().getTarget(),
                equalTo(ShapeId.from("smithy.api#String")));
    }

    @Test
    public void loadIsCleanForInlineCollections() {
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("main.smithy",
                        "$version: \"2.1\"\n"
                                + "namespace smithy.example\n"
                                + "structure S {\n"
                                + "    a: [String]\n"
                                + "    b: {String: Integer}\n"
                                + "}\n")
                .assemble();

        List<String> errors = result.getValidationEvents()
                .stream()
                .filter(event -> event.getSeverity().name().equals("ERROR"))
                .map(event -> event.getMessage())
                .collect(Collectors.toList());
        assertThat(errors, hasSize(0));
    }

    // A rare, pathological collision that the naming scheme does not resolve: two foreign
    // targets whose flattened namespace and name coincide (`com.amazon#String` and
    // `com#amazon_String` both flatten to `com_amazon_String`). Rather than silently reuse
    // the first synthetic shape, the two definitions conflict and the assembler's shape
    // conflict validation reports it, the same as any other conflicting shape definition.
    @Test
    public void collidingSyntheticNamesAreReportedNotSilentlyReused() {
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("targets.smithy",
                        "$version: \"2.1\"\n"
                                + "namespace com.amazon\n"
                                + "structure String {}\n")
                .addUnparsedModel("targets2.smithy",
                        "$version: \"2.1\"\n"
                                + "namespace com\n"
                                + "structure amazon_String {}\n")
                .addUnparsedModel("main.smithy",
                        "$version: \"2.1\"\n"
                                + "namespace smithy.example\n"
                                + "structure S {\n"
                                + "    a: [com.amazon#String]\n"
                                + "    b: [com#amazon_String]\n"
                                + "}\n")
                .assemble();

        List<String> errors = result.getValidationEvents()
                .stream()
                .filter(event -> event.getSeverity().name().equals("ERROR"))
                .map(event -> event.getMessage())
                .filter(message -> message.contains("Conflicting shape definition for")
                        && message.contains("_SyntheticListOf_com_amazon_String"))
                .collect(Collectors.toList());
        assertThat(errors, hasSize(1));
    }
}
