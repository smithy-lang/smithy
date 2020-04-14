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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

public class MockIntegrationTraitTest {
    @Test
    public void loadsValidTrait() {
        MockIntegrationTrait trait = MockIntegrationTrait.builder()
                .contentHandling("CONVERT_TO_TEXT")
                .passThroughBehavior("when_no_templates")
                .putRequestParameter("x", "y")
                .putRequestTemplate("application/json", "{}")
                .putRequestParameter("foo", "baz")
                .putResponse("[A-Z]+", IntegrationResponse.builder()
                        .statusCode("200")
                        .contentHandling("CONVERT_TO_TEXT")
                        .build())
                .build();

        assertThat(trait.toBuilder().build(), equalTo(trait));
        // Test round-tripping from/to node.
        assertThat(new MockIntegrationTrait.Provider().createTrait(ShapeId.from("com.foo#Baz"), trait.toNode()),
                   equalTo(trait));
    }

    @Test
    public void loadsTraitFromModel() {
        Model model = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(TestRunnerTest.class.getResource("errorfiles/valid-integration.json"))
                .assemble()
                .unwrap();

        MockIntegrationTrait trait = model.expectShape(ShapeId.from("ns.foo#Operation"))
                .getTrait(MockIntegrationTrait.class)
                .get();

        assertThat(trait.toBuilder().build(), equalTo(trait));

        // Test round-tripping from/to node.
        assertThat(new MockIntegrationTrait.Provider().createTrait(ShapeId.from("ns.foo#Operation"), trait.toNode()),
                   equalTo(trait));
    }
}
