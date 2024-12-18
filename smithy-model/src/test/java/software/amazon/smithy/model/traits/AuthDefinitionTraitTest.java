/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class AuthDefinitionTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode values = Node.fromStrings(
                JsonNameTrait.ID.toString(),
                XmlNameTrait.ID.toString());
        Node node = Node.objectNode().withMember("traits", values);
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#authDefinition"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(AuthDefinitionTrait.class));
        AuthDefinitionTrait authDefinitionTrait = (AuthDefinitionTrait) trait.get();
        assertThat(authDefinitionTrait.getTraits(),
                containsInAnyOrder(
                        JsonNameTrait.ID,
                        XmlNameTrait.ID));
        assertThat(authDefinitionTrait.toNode(), equalTo(node));
        assertThat(authDefinitionTrait.toBuilder().build(), equalTo(authDefinitionTrait));
    }
}
