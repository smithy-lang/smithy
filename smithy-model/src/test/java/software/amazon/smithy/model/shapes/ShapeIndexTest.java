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

package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class ShapeIndexTest {
    @Test
    public void hasShapes() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        ShapeIndex index = ShapeIndex.builder().addShape(a).build();

        assertTrue(index.getShape(ShapeId.from("ns.foo#a")).isPresent());
        assertFalse(index.getShape(ShapeId.from("ns.foo#baz")).isPresent());
    }

    @Test
    public void getsShapesAsType() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        ShapeIndex index = ShapeIndex.builder()
                .addShape(a)
                .addShape(b)
                .addShape(c)
                .build();
        List<StringShape> shapes = index.shapes(StringShape.class).collect(Collectors.toList());

        assertThat(shapes, hasSize(2));
        assertThat(shapes, hasItem(a));
        assertThat(shapes, hasItem(b));
    }

    @Test
    public void createsIndexFromCollection() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        ShapeIndex index = ShapeIndex.builder().addShapes(Arrays.asList(a, b, c)).build();
        List<Shape> shapes = index.shapes().collect(Collectors.toList());

        assertThat(shapes, hasSize(3));
        assertThat(shapes, hasItem(a));
        assertThat(shapes, hasItem(b));
        assertThat(shapes, hasItem(c));
    }

    @Test
    public void canRemoveShapeById() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        ShapeIndex index = ShapeIndex.builder()
                .addShapes(Arrays.asList(a, b))
                .removeShape(ShapeId.from("ns.foo#a"))
                .build();
        List<Shape> shapes = index.shapes().collect(Collectors.toList());

        assertThat(shapes, hasSize(1));
        assertThat(shapes, hasItem(b));
        assertThat(shapes, not(hasItem(a)));
    }

    @Test
    public void createsIndexFromIndex() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        ShapeIndex shapeIndexA = ShapeIndex.builder().addShapes(Arrays.asList(a, b, c)).build();
        ShapeIndex shapeIndexB = ShapeIndex.builder().addShapes(shapeIndexA).build();
        List<Shape> shapesA = shapeIndexA.shapes().collect(Collectors.toList());
        List<Shape> shapesB = shapeIndexB.shapes().collect(Collectors.toList());

        assertEquals(shapesA, shapesB);
        assertEquals(shapeIndexA, shapeIndexB);
        assertEquals(shapeIndexA.hashCode(), shapeIndexB.hashCode());
    }

    @Test
    public void comparesIndex() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        TimestampShape c = TimestampShape.builder().id("ns.foo#c").build();
        ShapeIndex shapeIndexA = ShapeIndex.builder().addShapes(Arrays.asList(a, b, c)).build();
        ShapeIndex shapeIndexB = ShapeIndex.builder().addShapes(shapeIndexA).build();

        assertEquals(shapeIndexA, shapeIndexB);
    }

    @Test
    public void differentiatesIndexes() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        ShapeIndex shapeIndexA = ShapeIndex.builder().addShapes(Arrays.asList(a, b)).build();
        ShapeIndex shapeIndexB = ShapeIndex.builder().addShape(a).build();

        assertNotEquals(shapeIndexA, shapeIndexB);
        assertNotEquals(shapeIndexA.hashCode(), shapeIndexB.hashCode());
    }

    @Test
    public void computesHashCode() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        ShapeIndex index = ShapeIndex.builder().addShape(a).build();

        assertThat(index.hashCode(), not(0));
    }

    @Test
    public void convertsToSet() {
        StringShape a = StringShape.builder().id("ns.foo#a").build();
        StringShape b = StringShape.builder().id("ns.foo#b").build();
        ShapeIndex index = ShapeIndex.builder().addShapes(a, b).build();

        assertThat(index.toSet(), containsInAnyOrder(a, b));
    }
}
