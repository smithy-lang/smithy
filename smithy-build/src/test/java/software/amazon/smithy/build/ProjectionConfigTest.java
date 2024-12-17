/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.TransformConfig;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.ListUtils;

public class ProjectionConfigTest {
    @Test
    public void buildsProjections() {
        TransformConfig t = TransformConfig.builder()
                .name("foo")
                .args(Node.objectNode().withMember("__args", Node.fromStrings("baz")))
                .build();
        ProjectionConfig p = ProjectionConfig.builder()
                .setAbstract(false)
                .transforms(ListUtils.of(t))
                .build();

        assertThat(p.getTransforms(), contains(t));
        assertFalse(p.isAbstract());
    }
}
