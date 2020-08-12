/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;

public class IntegrationTraitTest {
    @Test
    public void loadsValidTrait() {
        IntegrationTrait trait = IntegrationTrait.builder()
                .type("aws_proxy")
                .uri("foo")
                .httpMethod("POST")
                .addCacheKeyParameter("foo")
                .cacheNamespace("baz")
                .connectionId("id")
                .connectionType("xyz")
                .contentHandling("CONVERT_TO_TEXT")
                .credentials("abc")
                .payloadFormatVersion("1.0")
                .passThroughBehavior("when_no_templates")
                .putRequestParameter("x", "y")
                .build();

        assertThat(trait.toBuilder().build(), equalTo(trait));
        // Test round-tripping from/to node.
        assertThat(new IntegrationTrait.Provider().createTrait(ShapeId.from("ns.foo#Operation"), trait.toNode()),
                   equalTo(trait));
    }
}
