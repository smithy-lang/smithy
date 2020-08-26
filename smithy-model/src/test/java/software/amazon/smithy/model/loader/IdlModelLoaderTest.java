/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidatedResultException;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

public class IdlModelLoaderTest {
    @Test
    public void loadsAppropriateSourceLocations() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/main-test.smithy"))
                .assemble()
                .unwrap();

        model.shapes().forEach(shape -> {
            if (!Prelude.isPreludeShape(shape.getId())) {
                assertThat(shape.getSourceLocation(), not(equalTo(SourceLocation.NONE)));
            }

            // Non-member shapes defined in the main-test.smithy file should
            // all have a source location column of 1. The endsWith check is
            // necessary to filter out the prelude.
            if (shape.getSourceLocation().getFilename().endsWith("main-test.smithy") && !shape.isMemberShape()) {
                assertThat(shape.getSourceLocation().getColumn(), equalTo(1));
            }
        });
    }

    @Test
    public void fallsBackToPublicPreludeShapes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/forward-reference-resolver.smithy"))
                .assemble()
                .unwrap();

        MemberShape baz = model.expectShape(ShapeId.from("smithy.example#Foo$baz"))
                .asMemberShape().get();
        MemberShape bar = model.expectShape(ShapeId.from("smithy.example#Foo$bar"))
                .asMemberShape().get();
        ResourceShape resource = model.expectShape(ShapeId.from("smithy.example#MyResource"))
                .asResourceShape().get();

        assertThat(baz.getTarget().toString(), equalTo("smithy.api#String"));
        assertThat(bar.getTarget().toString(), equalTo("smithy.example#Integer"));
        assertThat(resource.getIdentifiers().get("a"), equalTo(ShapeId.from("smithy.example#MyString")));
        assertThat(resource.getIdentifiers().get("b"), equalTo(ShapeId.from("smithy.example#AnotherString")));
        assertThat(resource.getIdentifiers().get("c"), equalTo(ShapeId.from("smithy.api#String")));
    }

    @Test
    public void canLoadAndAliasShapesAndTraits() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("first-namespace.smithy"))
                .addImport(getClass().getResource("second-namespace.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void defersApplyTargetAndTrait() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("apply-use-1.smithy"))
                .addImport(getClass().getResource("apply-use-2.smithy"))
                .addImport(getClass().getResource("apply-use-3.smithy"))
                .assemble()
                .unwrap();

        Shape shape = model.expectShape(ShapeId.from("smithy.example#Foo"));

        assertThat(shape.findTrait(ShapeId.from("smithy.example#bar")), not(Optional.empty()));
        assertThat(shape.findTrait(ShapeId.from("smithy.example.b#baz")), not(Optional.empty()));
    }

    @Test
    public void limitsRecursion() {
        StringBuilder nodeBuilder = new StringBuilder("metadata foo = ");
        for (int i = 0; i < 251; i++) {
            nodeBuilder.append('[');
        }
        nodeBuilder.append("true");
        for (int i = 0; i < 251; i++) {
            nodeBuilder.append(']');
        }
        nodeBuilder.append("\n");

        ValidatedResultException e = Assertions.assertThrows(ValidatedResultException.class, () -> {
            Model.assembler().addUnparsedModel("/foo.smithy", nodeBuilder.toString()).assemble().unwrap();
        });

        assertThat(e.getMessage(), containsString("Parser exceeded the maximum allowed depth of"));
    }

    @Test
    public void handlesMultilineDocComments() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("multiline-comments.smithy"))
                .assemble()
                .unwrap();

        Shape shape = model.expectShape(ShapeId.from("smithy.example#MyStruct$myMember"));
        String docs = shape.getTrait(DocumentationTrait.class).map(StringTrait::getValue).orElse("");

        assertThat(docs, equalTo("This is the first line.\nThis is the second line."));
    }

    @Test
    public void warnsWhenInvalidSyntacticShapeIdIsFound() {
        ValidatedResult<Model> result = Model.assembler()
                .addUnparsedModel("foo.smithy", "namespace smithy.example\n"
                                                + "@tags([nonono])\n"
                                                + "string Foo\n")
                .assemble();

        assertThat(result.isBroken(), is(true));
        List<ValidationEvent> events = result.getValidationEvents(Severity.DANGER);
        assertThat(events.stream().filter(e -> e.getId().equals("SyntacticShapeIdTarget")).count(), equalTo(1L));
        assertThat(events.stream()
                           .filter(e -> e.getId().equals("SyntacticShapeIdTarget"))
                           .filter(e -> e.getMessage().contains("`nonono`"))
                           .count(),
                   equalTo(1L));
    }

    @Test
    public void properlyLoadsOperationsWithUseStatements() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("use-operations/service.smithy"))
                .addImport(getClass().getResource("use-operations/nested.smithy"))
                .addImport(getClass().getResource("use-operations/other.smithy"))
                .assemble()
                .unwrap();

        // Spot check for a specific "use" shape.
        assertThat(model.expectShape(ShapeId.from("smithy.example#Local"), OperationShape.class).getInput(),
                   equalTo(Optional.of(ShapeId.from("smithy.example.nested#A"))));

        assertThat(model.expectShape(ShapeId.from("smithy.example#Local"), OperationShape.class).getErrors(),
                   equalTo(ListUtils.of(ShapeId.from("smithy.example.nested#C"))));

        Map<String, ShapeId> identifiers = model.expectShape(
                ShapeId.from("smithy.example.nested#Resource"),
                ResourceShape.class
        ).getIdentifiers();

        assertThat(identifiers.get("s"), equalTo(ShapeId.from("smithy.api#String")));
        assertThat(identifiers.get("x"), equalTo(ShapeId.from("smithy.example.other#X")));
    }
}
