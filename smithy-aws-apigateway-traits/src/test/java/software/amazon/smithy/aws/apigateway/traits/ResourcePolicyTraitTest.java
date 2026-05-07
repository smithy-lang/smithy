/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.apigateway.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class ResourcePolicyTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        ShapeId id = ShapeId.from("smithy.example#Foo");
        ObjectNode policyNode = Node.objectNodeBuilder()
                .withMember("Version", "2012-10-17")
                .withMember("Statement",
                        Node.arrayNode(
                                Node.objectNodeBuilder()
                                        .withMember("Effect", "Allow")
                                        .withMember("Principal", "*")
                                        .withMember("Action", "execute-api:Invoke")
                                        .withMember("Resource",
                                                Node.arrayNode(Node.from("execute-api:/*")))
                                        .build()))
                .build();
        Trait trait = factory.createTrait(ResourcePolicyTrait.ID, id, policyNode).get();

        assertThat(trait, instanceOf(ResourcePolicyTrait.class));
        assertThat(((ResourcePolicyTrait) trait).getValue(), equalTo(policyNode));
        assertThat(factory.createTrait(ResourcePolicyTrait.ID, id, trait.toNode()).get(), equalTo(trait));
    }

    @Test
    public void roundTripsFromNode() {
        ObjectNode policyNode = Node.objectNodeBuilder()
                .withMember("Version", "2012-10-17")
                .build();
        ResourcePolicyTrait trait = new ResourcePolicyTrait(policyNode);

        assertThat(trait.toNode(), equalTo(policyNode));
        assertThat(new ResourcePolicyTrait.Provider()
                .createTrait(ShapeId.from("ns.foo#Bar"), trait.toNode()),
                equalTo(trait));
    }
}
