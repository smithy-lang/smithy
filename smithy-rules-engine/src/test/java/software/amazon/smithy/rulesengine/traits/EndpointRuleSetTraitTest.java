/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.traits;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.TraitFactory;

public final class EndpointRuleSetTraitTest {
    @Test
    public void indexesTheModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("traits-test-model.smithy"))
                .assemble()
                .unwrap();

        ServiceShape serviceShape = result.expectShape(ShapeId.from("smithy.example#ExampleService"), ServiceShape.class);

        EndpointRuleSetTrait ruleSetTrait = serviceShape.getTrait(EndpointRuleSetTrait.class).get();

        ObjectNode document = ruleSetTrait.getRuleSet().expectObjectNode();

        assertEquals(document.expectStringMember("version").getValue(), "1.0");
    }

    @Test
    public void roundTrips() {
        Node expectedNode = Node.parse(
                "{\"version\":\"1.0\",\"parameters\":{\"stringParam\":{\"type\":\"string\"}"
                + ",\"booleanParam\":{\"type\":\"boolean\"}},\"rules\":[]}");

        TraitFactory traitFactory = TraitFactory.createServiceFactory();
        EndpointRuleSetTrait expectedTrait = (EndpointRuleSetTrait) traitFactory.createTrait(EndpointRuleSetTrait.ID,
                ShapeId.from("ns.example#Foo"), expectedNode).get();

        EndpointRuleSetTrait actualTrait = expectedTrait.toBuilder().build();
        assertThat(expectedTrait, equalTo(actualTrait));

        assertThat(expectedNode, equalTo(expectedTrait.toNode()));
    }
}
