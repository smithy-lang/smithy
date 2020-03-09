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
