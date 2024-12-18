/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import java.util.List;
import org.junit.jupiter.api.Test;

public class OperationShapeTest {
    @Test
    public void combinesErrorsWithServiceErrors() {
        ServiceShape service = ServiceShape.builder()
                .id("com.foo#Example")
                .version("x")
                .addError("com.foo#Common1")
                .addError(ShapeId.from("com.foo#Common2"))
                .build();

        OperationShape operation = OperationShape.builder()
                .id("com.foo#Operation")
                .addError("com.foo#OperationError")
                .build();

        List<ShapeId> allErrors = operation.getErrors(service);

        assertThat(allErrors,
                contains(
                        ShapeId.from("com.foo#Common1"),
                        ShapeId.from("com.foo#Common2"),
                        ShapeId.from("com.foo#OperationError")));
    }
}
