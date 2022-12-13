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

        assertThat(allErrors, contains(
                ShapeId.from("com.foo#Common1"),
                ShapeId.from("com.foo#Common2"),
                ShapeId.from("com.foo#OperationError")));
    }
}
