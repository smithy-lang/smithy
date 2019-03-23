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

package software.amazon.smithy.model.selector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeIndex;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;

public class ShapeTypeSelectorTest {
    @Test
    public void matchesByType() {
        Selector selector = new ShapeTypeSelector(ShapeType.STRING);
        Shape a = IntegerShape.builder().id("foo.baz#Bar").build();
        Shape b = StringShape.builder().id("foo.baz#Bam").build();
        ShapeIndex index = ShapeIndex.builder().addShapes(a, b).build();
        Set<Shape> result = selector.select(index);

        assertThat(result, contains(b));
    }
}
