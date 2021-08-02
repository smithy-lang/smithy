/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;

public class PendingShapeTest {
    @Test
    public void cannotMergeIncompatibleShapes() {
        PendingShape a = PendingShape.create(
                ShapeId.from("test#A"),
                SourceLocation.NONE,
                SetUtils.of(),
                (created) -> {});
        PendingShape b = PendingShape.create(
                ShapeId.from("test#B"),
                SourceLocation.NONE,
                SetUtils.of(),
                (created) -> {});

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            PendingShape.mergeIntoLeft(a, b);
        });
    }

    @Test
    public void mergesIntoExistingPendingConflict() {
        PendingShape a = PendingShape.create(
                ShapeId.from("test#A"),
                SourceLocation.NONE,
                SetUtils.of(ShapeId.from("test#X")),
                (created) -> {});
        PendingShape b = PendingShape.create(
                ShapeId.from("test#A"),
                SourceLocation.NONE,
                SetUtils.of(ShapeId.from("test#Y")),
                (created) -> {});

        PendingShape merged = PendingShape.mergeIntoLeft(PendingShape.mergeIntoLeft(a, b), a);

        assertThat(merged.getPendingShapes(), containsInAnyOrder(ShapeId.from("test#X"), ShapeId.from("test#Y")));
    }

    @Test
    public void callsOnlyTheFirstMergedBuilder() {
        Set<String> called = new HashSet<>();
        PendingShape a = PendingShape.create(
                ShapeId.from("test#A"),
                SourceLocation.NONE,
                SetUtils.of(ShapeId.from("test#X")),
                (created) -> called.add("a"));
        PendingShape b = PendingShape.create(
                ShapeId.from("test#A"),
                SourceLocation.NONE,
                SetUtils.of(ShapeId.from("test#Y")),
                (created) -> called.add("b"));
        PendingShape merged = PendingShape.mergeIntoLeft(a, b);
        merged.buildShapes(Collections.emptyMap());

        assertThat(called, contains("a", "b"));
    }

    @Test
    public void findsUnresolvedShapesForEachPendingShape() {
        PendingShape a = PendingShape.create(
                ShapeId.from("test#A"),
                SourceLocation.NONE,
                SetUtils.of(ShapeId.from("test#X")),
                (created) -> {});
        PendingShape b = PendingShape.create(
                ShapeId.from("test#A"),
                SourceLocation.NONE,
                SetUtils.of(ShapeId.from("test#Y")),
                (created) -> {});
        PendingShape merged = PendingShape.mergeIntoLeft(a, b);

        List<ValidationEvent> events = merged.unresolved(Collections.emptyMap(), Collections.emptyMap());

        assertThat(events, hasSize(2));
    }
}
