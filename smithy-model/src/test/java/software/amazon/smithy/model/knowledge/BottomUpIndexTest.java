/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EntityShape;
import software.amazon.smithy.model.shapes.ShapeId;

public class BottomUpIndexTest {
    private static Model model;
    private static ShapeId serviceId = ShapeId.from("smithy.example#Example");

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(OperationIndexTest.class.getResource("bottom-up.smithy"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void findsEntityBinding() {
        BottomUpIndex index = BottomUpIndex.of(model);

        assertThat(index.getEntityBinding(serviceId, serviceId), equalTo(Optional.empty()));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#ServiceOperation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(serviceId)));
        assertThat(index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1")).map(EntityShape::getId),
                equalTo(Optional.of(serviceId)));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_1")).map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_2")).map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_1")).map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_2")).map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_1_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1_1"))));
        assertThat(
                index.getEntityBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_2_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1_2"))));
    }

    @Test
    public void findsResourceBinding() {
        BottomUpIndex index = BottomUpIndex.of(model);

        assertThat(index.getResourceBinding(serviceId, serviceId), equalTo(Optional.empty()));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#ServiceOperation"))
                        .map(EntityShape::getId),
                equalTo(Optional.empty()));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1")).map(EntityShape::getId),
                equalTo(Optional.empty()));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_1")).map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_2")).map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_1"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_2"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_1_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1_1"))));
        assertThat(
                index.getResourceBinding(serviceId, ShapeId.from("smithy.example#Resource1_1_2_Operation"))
                        .map(EntityShape::getId),
                equalTo(Optional.of(ShapeId.from("smithy.example#Resource1_1_2"))));
    }

    @Test
    public void findsPathToLeafOperation() {
        BottomUpIndex index = BottomUpIndex.of(model);
        List<EntityShape> entities = index.getAllParents(
                serviceId,
                ShapeId.from("smithy.example#Resource1_1_2_Operation"));
        List<String> ids = entities.stream()
                .map(EntityShape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toList());

        assertThat(ids,
                contains("smithy.example#Resource1_1_2",
                        "smithy.example#Resource1_1",
                        "smithy.example#Resource1",
                        "smithy.example#Example"));
    }

    @Test
    public void findsPathToLeafResource() {
        BottomUpIndex index = BottomUpIndex.of(model);
        List<EntityShape> entities = index.getAllParents(
                serviceId,
                ShapeId.from("smithy.example#Resource1_1_2"));
        List<String> ids = entities.stream()
                .map(EntityShape::getId)
                .map(ShapeId::toString)
                .collect(Collectors.toList());

        assertThat(ids, contains("smithy.example#Resource1_1", "smithy.example#Resource1", "smithy.example#Example"));
    }
}
