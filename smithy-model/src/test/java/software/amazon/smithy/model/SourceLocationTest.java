/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

public class SourceLocationTest {
    @Test
    public void sortsSourceLocations() {
        Model model = Model.builder()
                .addShape(StringShape.builder()
                        .id("smithy.example#First")
                        .source(new SourceLocation("a.smithy", 1, 1))
                        .build())
                .addShape(StringShape.builder()
                        .id("smithy.example#Second")
                        .source(new SourceLocation("a.smithy", 1, 2))
                        .build())
                .addShape(StringShape.builder()
                        .id("smithy.example#Third")
                        .source(new SourceLocation("a.smithy", 2, 1))
                        .build())
                .addShape(StringShape.builder()
                        .id("smithy.example#Fourth")
                        .source(new SourceLocation("b.smithy", 1, 1))
                        .build())
                .build();

        List<Shape> shapes = model.shapes()
                .sorted(Comparator.comparing(Shape::getSourceLocation))
                .collect(Collectors.toList());
        assertEquals("First", shapes.get(0).getId().getName());
        assertEquals("Second", shapes.get(1).getId().getName());
        assertEquals("Third", shapes.get(2).getId().getName());
        assertEquals("Fourth", shapes.get(3).getId().getName());
    }
}
