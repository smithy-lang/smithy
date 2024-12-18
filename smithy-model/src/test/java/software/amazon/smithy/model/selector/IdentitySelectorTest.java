/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

public class IdentitySelectorTest {
    @Test
    public void createdFromSelectorParse() {
        Selector s = Selector.parse("*");

        assertThat(s, instanceOf(IdentitySelector.class));
    }

    @Test
    public void returnsAllShapes() {
        Selector s = Selector.parse("*");
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.foo#A").build())
                .addShape(StringShape.builder().id("com.foo#B").build())
                .build();

        assertThat(s.select(model), equalTo(model.toSet()));
    }

    @Test
    public void returnsAllShapesInStream() {
        Selector s = Selector.parse("*");
        Model model = Model.builder()
                .addShape(StringShape.builder().id("com.foo#A").build())
                .addShape(StringShape.builder().id("com.foo#B").build())
                .build();

        assertThat(s.shapes(model).collect(Collectors.toSet()), equalTo(model.toSet()));
    }

    @Test
    public void returnsGivenClosureShapes() {
        Selector s = Selector.parse("*");
        Shape shape1 = StringShape.builder().id("com.foo#A").build();
        Shape shape2 = StringShape.builder().id("com.foo#B").build();
        Model model = Model.builder().addShapes(shape1, shape2).build();

        Set<Shape> given = Collections.singleton(shape1);
        Selector.StartingContext env = new Selector.StartingContext(given);

        assertThat(s.select(model, env), equalTo(given));
    }
}
