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

package software.amazon.smithy.build;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class ProjectionResultTest {
    @Test
    public void checksIfBrokenBasedOnError() {
        assertTrue(ProjectionResult.builder()
                .projectionName("foo")
                .model(Model.builder().build())
                .addEvent(ValidationEvent.builder()
                        .message("foo")
                        .severity(Severity.ERROR)
                        .id("abc")
                        .build())
                .build().isBroken());
    }

    @Test
    public void checksIfBrokenBasedOnDanger() {
        assertTrue(ProjectionResult.builder()
                .projectionName("foo")
                .model(Model.builder().build())
                .addEvent(ValidationEvent.builder()
                        .message("foo")
                        .severity(Severity.DANGER)
                        .id("abc")
                        .build())
                .build().isBroken());
    }
}
