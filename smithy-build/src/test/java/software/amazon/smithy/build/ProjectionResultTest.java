/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
                .build()
                .isBroken());
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
                .build()
                .isBroken());
    }
}
