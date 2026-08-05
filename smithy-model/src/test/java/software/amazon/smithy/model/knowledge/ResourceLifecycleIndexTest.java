/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class ResourceLifecycleIndexTest {
    @Test
    public void indexesCreatesResources() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("resource-lifecycle-index.smithy"))
                .assemble()
                .unwrap();
        ResourceLifecycleIndex index = ResourceLifecycleIndex.of(model);

        // Operation -> resources
        assertThat(index.getResources(ShapeId.from("com.example#CreateBoth"),
                ResourceLifecycleIndex.Lifecycle.CREATE),
                contains(ShapeId.from("com.example#Foo"), ShapeId.from("com.example#Bar")));

        // Resource -> operations (inverse)
        assertThat(index.getOperations(ShapeId.from("com.example#Foo"),
                ResourceLifecycleIndex.Lifecycle.CREATE),
                contains(ShapeId.from("com.example#CreateBoth")));

        assertThat(index.getOperations(ShapeId.from("com.example#Bar"),
                ResourceLifecycleIndex.Lifecycle.CREATE),
                contains(ShapeId.from("com.example#CreateBoth")));

        // Delete operation
        assertThat(index.getResources(ShapeId.from("com.example#DeleteFoo"),
                ResourceLifecycleIndex.Lifecycle.DELETE),
                contains(ShapeId.from("com.example#Foo")));

        // No results for unrelated queries
        assertThat(index.getResources(ShapeId.from("com.example#CreateBoth"),
                ResourceLifecycleIndex.Lifecycle.DELETE),
                is(empty()));
    }

    @Test
    public void indexesAllLifecycleTypes() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("resource-lifecycle-index.smithy"))
                .assemble()
                .unwrap();
        ResourceLifecycleIndex index = ResourceLifecycleIndex.of(model);

        assertThat(index.getResources(ShapeId.from("com.example#PutFoo"),
                ResourceLifecycleIndex.Lifecycle.PUT),
                contains(ShapeId.from("com.example#Foo")));
        assertThat(index.getResources(ShapeId.from("com.example#ReadBar"),
                ResourceLifecycleIndex.Lifecycle.READ),
                contains(ShapeId.from("com.example#Bar")));
        assertThat(index.getResources(ShapeId.from("com.example#UpdateBar"),
                ResourceLifecycleIndex.Lifecycle.UPDATE),
                contains(ShapeId.from("com.example#Bar")));

        // Inverse lookups.
        assertThat(index.getOperations(ShapeId.from("com.example#Foo"),
                ResourceLifecycleIndex.Lifecycle.PUT),
                contains(ShapeId.from("com.example#PutFoo")));
        assertThat(index.getOperations(ShapeId.from("com.example#Bar"),
                ResourceLifecycleIndex.Lifecycle.UPDATE),
                contains(ShapeId.from("com.example#UpdateBar")));
    }

    @Test
    public void returnsAllLifecyclesForResourceAndOperation() {
        Model model = Model.assembler()
                .addImport(getClass().getResource("resource-lifecycle-index.smithy"))
                .assemble()
                .unwrap();
        ResourceLifecycleIndex index = ResourceLifecycleIndex.of(model);

        // Bar is read and updated across two operations.
        Map<ResourceLifecycleIndex.Lifecycle, Set<ShapeId>> barOps =
                index.getOperations(ShapeId.from("com.example#Bar"));
        assertThat(barOps.keySet(),
                containsInAnyOrder(
                        ResourceLifecycleIndex.Lifecycle.CREATE,
                        ResourceLifecycleIndex.Lifecycle.READ,
                        ResourceLifecycleIndex.Lifecycle.UPDATE));

        // CreateBoth creates two resources under the CREATE lifecycle.
        Map<ResourceLifecycleIndex.Lifecycle, Set<ShapeId>> createBothResources =
                index.getResources(ShapeId.from("com.example#CreateBoth"));
        assertThat(createBothResources.get(ResourceLifecycleIndex.Lifecycle.CREATE),
                containsInAnyOrder(ShapeId.from("com.example#Foo"), ShapeId.from("com.example#Bar")));
    }
}
