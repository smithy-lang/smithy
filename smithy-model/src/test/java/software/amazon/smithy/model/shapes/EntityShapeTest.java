/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.ListUtils;

public class EntityShapeTest {
    @Test
    public void operationsAreOrdered() {
        ServiceShape.Builder builder = ServiceShape.builder()
                .id("com.foo#Bar")
                .version("1");

        List<ShapeId> operations = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            OperationShape operation = OperationShape.builder().id("com.foo#Op" + i).build();
            operations.add(operation.getId());
            builder.addOperation(operation);
        }

        ServiceShape service = builder.build();

        assertThat(ListUtils.copyOf(service.getAllOperations()), equalTo(operations));
    }

    @Test
    public void resourcesAreOrdered() {
        ServiceShape.Builder builder = ServiceShape.builder()
                .id("com.foo#Bar")
                .version("1");

        List<ShapeId> resources = new ArrayList<>(20);
        for (int i = 0; i < 20; i++) {
            ResourceShape resource = ResourceShape.builder().id("com.foo#Resource" + i).build();
            resources.add(resource.getId());
            builder.addResource(resource);
        }

        ServiceShape service = builder.build();

        assertThat(ListUtils.copyOf(service.getResources()), equalTo(resources));
    }
}
