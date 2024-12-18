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

public class AuthorizersTraitTest {
    @Test
    public void registersTrait() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        ShapeId id = ShapeId.from("smithy.example#Foo");
        ObjectNode node = Node.objectNodeBuilder()
                .withMember("aws.v4",
                        Node.objectNodeBuilder()
                                .withMember("scheme", "aws.auth#sigv4")
                                .withMember("type", "request")
                                .withMember("uri", "arn:foo:baz")
                                .withMember("credentials", "arn:foo:bar")
                                .withMember("identitySource", "mapping.expression")
                                .withMember("identityValidationExpression", "[A-Z]+")
                                .withMember("resultTtlInSeconds", 100)
                                .withMember("authorizerPayloadFormatVersion", "format.version")
                                .withMember("enableSimpleResponse", true)
                                .build())
                .build();
        Trait trait = factory.createTrait(AuthorizersTrait.ID, id, node).get();

        assertThat(trait, instanceOf(AuthorizersTrait.class));
        assertThat(factory.createTrait(AuthorizersTrait.ID, id, trait.toNode()).get(), equalTo(trait));
    }
}
