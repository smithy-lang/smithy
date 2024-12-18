/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.SensitiveTrait;

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
        Model current = Model.builder().addShapes(shape).build();
        Differences differences = Differences.detect(previous, current);

        assertThat(differences.addedShapes().count(), equalTo(1L));
        assertThat(differences.addedShapes(StringShape.class).count(), equalTo(1L));
    }

    @Test
    public void detectsRemovedShapes() {
        Shape shape = StringShape.builder().id("foo.bar#Baz").build();
        Model previous = Model.builder().addShapes(shape).build();
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
                .addTrait(new SensitiveTrait())
                .build();
        Model previous = Model.assembler().addShape(shape1).assemble().unwrap();
        Model current = Model.assembler().addShape(shape2).assemble().unwrap();
        Differences differences = Differences.detect(previous, current);

        assertThat(differences.changedShapes().count(), equalTo(1L));
        ChangedShape<Shape> diff = differences.changedShapes().findFirst().get();
        assertThat(diff.getOldShape(), equalTo(shape1));
        assertThat(diff.getNewShape(), equalTo(shape2));
    }
}
