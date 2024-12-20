/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

public class SmithyBuildResultTest {
    @Test
    public void checksIfAnyFailed() {
        assertTrue(SmithyBuildResult.builder()
                .addProjectionResult(ProjectionResult.builder()
                        .projectionName("foo")
                        .model(Model.builder().build())
                        .addEvent(ValidationEvent.builder()
                                .message("foo")
                                .severity(Severity.ERROR)
                                .id("abc")
                                .build())
                        .build())
                .build()
                .anyBroken());

        assertFalse(SmithyBuildResult.builder()
                .addProjectionResult(ProjectionResult.builder()
                        .model(Model.builder().build())
                        .projectionName("foo")
                        .build())
                .build()
                .anyBroken());
    }

    @Test
    public void convertsToMap() {
        ProjectionResult a = ProjectionResult.builder()
                .projectionName("a")
                .model(Model.builder().build())
                .build();
        ProjectionResult b = ProjectionResult.builder()
                .projectionName("b")
                .model(Model.builder().build())
                .build();
        SmithyBuildResult result = SmithyBuildResult.builder()
                .addProjectionResult(a)
                .addProjectionResult(b)
                .build();
        Map<String, ProjectionResult> map = result.getProjectionResultsMap();

        assertThat(map, hasKey("a"));
        assertThat(map, hasKey("b"));
        assertThat(map.get("a"), equalTo(a));
        assertThat(map.get("b"), equalTo(b));
    }
}
