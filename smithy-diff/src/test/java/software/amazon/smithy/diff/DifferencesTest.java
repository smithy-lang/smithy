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

package software.amazon.smithy.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.SensitiveTrait;
import software.amazon.smithy.model.traits.TraitDefinition;

public class DifferencesTest {
    @Test
    public void detectsAddedMetadata() {
        Model a = Model.builder().build();
        Model b = Model.builder().putMetadataProperty("foo", Node.from("bar")).build();
        Differences differences = Differences.detect(a, b);

        assertThat(differences.addedMetadata().count(), equalTo(1L));
        assertThat(differences.addedMetadata().findFirst().get().getLeft(), equalTo("foo"));
    }

    @Test
    public void detectsRemovedMetadata() {
        Model a = Model.builder().putMetadataProperty("foo", Node.from("bar")).build();
        Model b = Model.builder().build();
        Differences differences = Differences.detect(a, b);

        assertThat(differences.removedMetadata().count(), equalTo(1L));
        assertThat(differences.removedMetadata().findFirst().get().getLeft(), equalTo("foo"));
    }

    @Test
    public void detectsChangedMetadata() {
        Model a = Model.builder().putMetadataProperty("foo", Node.from("bar")).build();
        Model b = Model.builder().putMetadataProperty("foo", Node.from(10)).build();
        Differences differences = Differences.detect(a, b);

        assertThat(differences.changedMetadata().count(), equalTo(1L));
        assertThat(differences.changedMetadata().findFirst().get().getKey(), equalTo("foo"));
    }

    @Test
    public void detectsAddedShapes() {
        Shape shape = StringShape.builder().id("foo.bar#Baz").build();
        Model previous = Model.builder().build();
        Model current = Model.builder().shapeIndex(ShapeIndex.builder().addShapes(shape).build()).build();
        Differences differences = Differences.detect(previous, current);

        assertThat(differences.addedShapes().count(), equalTo(1L));
        assertThat(differences.addedShapes(StringShape.class).count(), equalTo(1L));
    }

    @Test
    public void detectsRemovedShapes() {
        Shape shape = StringShape.builder().id("foo.bar#Baz").build();
        Model previous = Model.builder().shapeIndex(ShapeIndex.builder().addShapes(shape).build()).build();
        Model current = Model.builder().build();
        Differences differences = Differences.detect(previous, current);

        assertThat(differences.removedShapes().count(), equalTo(1L));
        assertThat(differences.removedShapes(StringShape.class).count(), equalTo(1L));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void detectsChangedShapes() {
        Shape shape1 = StringShape.builder().id("foo.bar#Baz").build();
        Shape shape2 = StringShape.builder()
                .id("foo.bar#Baz")
                .addTrait(new SensitiveTrait(SourceLocation.NONE))
                .build();
        Model previous = Model.assembler().addShape(shape1).assemble().unwrap();
        Model current = Model.assembler().addShape(shape2).assemble().unwrap();
        Differences differences = Differences.detect(previous, current);

        assertThat(differences.changedShapes().count(), equalTo(1L));
        ChangedShape<Shape> diff = differences.changedShapes().findFirst().get();
        assertThat(diff.getOldShape(), equalTo(shape1));
        assertThat(diff.getNewShape(), equalTo(shape2));
    }

    @Test
    public void detectsAddedDefinitions() {
        Model a = Model.builder().build();
        Model b = Model.builder()
                .addTraitDefinition(TraitDefinition.builder().name("foo.baz#Trait").build())
                .build();
        Differences differences = Differences.detect(a, b);

        assertThat(differences.addedTraitDefinitions().count(), equalTo(1L));
        assertThat(differences.addedTraitDefinitions().findFirst().get().getFullyQualifiedName(),
                   equalTo("foo.baz#Trait"));
    }

    @Test
    public void detectsRemovedDefinitions() {
        Model a = Model.builder()
                .addTraitDefinition(TraitDefinition.builder().name("foo.baz#Trait").build())
                .build();
        Model b = Model.builder().build();
        Differences differences = Differences.detect(a, b);

        assertThat(differences.removedTraitDefinitions().count(), equalTo(1L));
        assertThat(differences.removedTraitDefinitions().findFirst().get().getFullyQualifiedName(),
                   equalTo("foo.baz#Trait"));
    }

    @Test
    public void detectsChangedDefinitions() {
        Model a = Model.builder()
                .addTraitDefinition(TraitDefinition.builder().name("foo.baz#Trait").build())
                .build();
        Model b = Model.builder()
                .addTraitDefinition(TraitDefinition.builder()
                        .name("foo.baz#Trait")
                        .structurallyExclusive(true)
                        .build())
                .build();
        Differences differences = Differences.detect(a, b);

        assertThat(differences.changedTraitDefinitions().count(), equalTo(1L));
    }
}
