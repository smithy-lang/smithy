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

package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;

public class EffectiveOperationResourceIndexTest {

    private static Model model;
    private static ShapeId serviceId = ShapeId.from("example.smithy#MyService");

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(EffectiveOperationResourceIndexTest.class.getResource("effective-resource-index-test.json"))
                .assemble()
                .unwrap();
    }

    @AfterAll
    public static void after() {
        model = null;
    }

    @Test
    public void targetsHostResourceForInstanceOperations() {
        ResourceShape childResource = model.getShapeIndex().getShape(ShapeId.from("example.smithy#ChildResource"))
                .flatMap(Shape::asResourceShape)
                .get();
        EffectiveOperationResourceIndex index = model.getKnowledge(EffectiveOperationResourceIndex.class);
        Optional<ResourceShape> result = index.getEffectiveResource(serviceId, childResource.getCreate().get());

        assertTrue(result.isPresent());
        assertThat(result.get(), is(childResource));
    }

    @Test
    public void targetsParentResourceForCollectionOperations() {
        ResourceShape someResource = model.getShapeIndex().getShape(ShapeId.from("example.smithy#SomeResource"))
                .flatMap(Shape::asResourceShape)
                .get();
        ResourceShape childResource = model.getShapeIndex().getShape(ShapeId.from("example.smithy#ChildResource"))
                .flatMap(Shape::asResourceShape)
                .get();
        assertTrue(someResource.getResources().contains(childResource.getId()));

        EffectiveOperationResourceIndex index = model.getKnowledge(EffectiveOperationResourceIndex.class);
        Optional<EffectiveOperationResourceIndex.EffectiveResourceBindings> result
                = index.getEffectiveResourceAndBindings(serviceId, childResource.getList().get());

        assertTrue(result.isPresent());
        assertThat(result.get().getResource(), is(someResource));
    }

    @Test
    public void targetsNothingForCollectionOperationsOnRootResources() {
        ResourceShape someResource = model.getShapeIndex().getShape(ShapeId.from("example.smithy#SomeResource"))
                .flatMap(Shape::asResourceShape)
                .get();
        EffectiveOperationResourceIndex index = model.getKnowledge(EffectiveOperationResourceIndex.class);
        Optional<EffectiveOperationResourceIndex.EffectiveResourceBindings> result
                = index.getEffectiveResourceAndBindings(serviceId, someResource.getList().get());

        assertFalse(result.isPresent());
    }

    @Test
    public void targetsNothingForOperationsNotBoundToResources() {
        EffectiveOperationResourceIndex index = model.getKnowledge(EffectiveOperationResourceIndex.class);
        Optional<EffectiveOperationResourceIndex.EffectiveResourceBindings> result
                = index.getEffectiveResourceAndBindings(serviceId, ShapeId.from("example.smithy#SomeOperation"));

        assertFalse(result.isPresent());
    }
}
