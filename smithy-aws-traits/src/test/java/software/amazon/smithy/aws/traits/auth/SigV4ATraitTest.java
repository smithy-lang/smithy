/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.traits.auth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class SigV4ATraitTest {
    private static final String MOCK_SIGNING_NAME = "mocksigningname";
    private static final ShapeId MOCK_TARGET = ShapeId.from("ns.qux#foo");

    @Test
    public void loadsTrait() {
        Node node = ObjectNode.builder()
            .withMember("name", StringNode.from(MOCK_SIGNING_NAME))
            .build();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(SigV4ATrait.ID, MOCK_TARGET, node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(SigV4ATrait.class));
        SigV4ATrait sigv4aTrait = (SigV4ATrait) trait.get();
        assertFalse(sigv4aTrait.getName().isEmpty());
        assertThat(sigv4aTrait.getName(), equalTo(MOCK_SIGNING_NAME));
        assertThat(sigv4aTrait.toNode(), equalTo(node));
        assertThat(sigv4aTrait.toBuilder().build(), equalTo(sigv4aTrait));
    }
}
