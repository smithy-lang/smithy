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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

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

        MemberShape baz = model.expectShape(ShapeId.from("smithy.example#Foo$baz")).expectMemberShape();
        MemberShape bar = model.expectShape(ShapeId.from("smithy.example#Foo$bar")).expectMemberShape();
        ResourceShape resource = model.expectShape(ShapeId.from("smithy.example#MyResource")).expectResourceShape();

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
}
