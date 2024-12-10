/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class ShapeTypeTest {
    @Test
    public void createsBuilders() {
        Shape result = ShapeType.STRING.createBuilderForType().id("example#Foo").build();

        assertThat(result.getType(), Matchers.is(ShapeType.STRING));
        assertThat(result, Matchers.instanceOf(StringShape.class));
    }

    @Test
    public void hasCategory() {
        assertThat(ShapeType.STRING.getCategory(), Matchers.is(ShapeType.Category.SIMPLE));
        assertThat(ShapeType.LIST.getCategory(), Matchers.is(ShapeType.Category.AGGREGATE));
        assertThat(ShapeType.SERVICE.getCategory(), Matchers.is(ShapeType.Category.SERVICE));
    }

    @Test
    public void enumEquivalence() {
        assertTrue(ShapeType.ENUM.isShapeType(ShapeType.STRING));
        assertFalse(ShapeType.STRING.isShapeType(ShapeType.ENUM));

        assertTrue(ShapeType.INT_ENUM.isShapeType(ShapeType.INTEGER));
        assertFalse(ShapeType.INTEGER.isShapeType(ShapeType.INT_ENUM));
    }
}
