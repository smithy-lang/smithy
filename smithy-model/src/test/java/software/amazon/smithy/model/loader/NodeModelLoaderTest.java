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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.DefaultNodeFactory;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitFactory;
import software.amazon.smithy.model.validation.Severity;

public class NodeModelLoaderTest {
    @Test
    public void validatesNamespaceNames() {
        Assertions.assertThrows(SourceException.class, () -> {
            LoaderVisitor visitor = new LoaderVisitor(TraitFactory.createServiceFactory());
            new NodeModelLoader(new DefaultNodeFactory())
                    .load("foo.json", () -> "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"foo.baz!\": {}}", visitor);
        });
    }

    @Test
    public void validatesShapeIds() {
        LoaderVisitor visitor = new LoaderVisitor(TraitFactory.createServiceFactory());
        new NodeModelLoader(new DefaultNodeFactory()).load(
                "foo.json",
                () -> "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"foo.baz\": {\"shapes\": {\"ns.bar#bam\": {\"type\": \"string\"}}}}",
                visitor);
        assertThat(visitor.onEnd().getValidationEvents(Severity.ERROR), not(empty()));
    }

    @Test
    public void validatesTraitShapeIds() {
        LoaderVisitor visitor = new LoaderVisitor(TraitFactory.createServiceFactory());
        new NodeModelLoader(new DefaultNodeFactory()).load(
                "foo.json",
                () -> "{\"smithy\": \"" + Model.MODEL_VERSION + "\", \"foo.baz\": {\"traits\": {\"ns.bar#bam\": {\"sensitive\": true}}}}",
                visitor);
        assertThat(visitor.onEnd().getValidationEvents(Severity.ERROR), not(empty()));
    }

    @Test
    public void fallsBackToPublicPreludeShapes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/forward-reference-resolver.json"))
                .assemble()
                .unwrap();

        MemberShape baz = model.getShapeIndex()
                .getShape(ShapeId.from("smithy.example#Foo$baz")).get()
                .asMemberShape().get();
        MemberShape bar = model.getShapeIndex()
                .getShape(ShapeId.from("smithy.example#Foo$bar")).get()
                .asMemberShape().get();
        ResourceShape resource = model.getShapeIndex().getShape(ShapeId.from("smithy.example#MyResource")).get()
                .asResourceShape().get();

        assertThat(baz.getTarget().toString(), equalTo("smithy.api#String"));
        assertThat(bar.getTarget().toString(), equalTo("smithy.example#Integer"));
        assertThat(resource.getIdentifiers().get("a"), equalTo(ShapeId.from("smithy.example#MyString")));
        assertThat(resource.getIdentifiers().get("b"), equalTo(ShapeId.from("smithy.example#AnotherString")));
        assertThat(resource.getIdentifiers().get("c"), equalTo(ShapeId.from("smithy.api#String")));
    }

    @Test
    public void loadsTraitDefinitions() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("valid/trait-definitions.json"))
                .assemble()
                .unwrap();

        assertTrue(model.getTraitDefinition("example.namespace#customTrait").isPresent());
        assertTrue(model.getTraitDefinition("example.namespace#documentation").isPresent());
        assertTrue(model.getTraitDefinition("example.namespace#numeric").isPresent());
        assertThat(model.getTraitShapes(),
                   hasItem(model.getShapeIndex().getShape(ShapeId.from("example.namespace#numeric")).get()));
    }
}
