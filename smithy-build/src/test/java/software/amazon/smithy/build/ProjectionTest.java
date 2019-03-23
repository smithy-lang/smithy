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
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProjectionTest {
    @Test
    public void projectionNamesMustBeValid() {
        Assertions.assertThrows(SmithyBuildException.class, () -> {
            Projection.builder().name("~!invalid!").build();
        });
    }

    @Test
    public void projectionRequiresName() {
        Assertions.assertThrows(SmithyBuildException.class, () -> Projection.builder().build());
    }

    @Test
    public void buildsProjections() {
        TransformConfiguration t = TransformConfiguration.builder()
                .name("foo")
                .args(List.of("baz"))
                .build();
        Projection p = Projection.builder()
                .name("foo")
                .isAbstract(false)
                .addTransform(t)
                .build();

        assertThat(p.getName(), equalTo("foo"));
        assertThat(p.getTransforms(), contains(t));
        assertFalse(p.isAbstract());
    }
}
