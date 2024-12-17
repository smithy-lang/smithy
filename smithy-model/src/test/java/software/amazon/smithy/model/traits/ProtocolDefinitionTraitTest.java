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

public class ProtocolDefinitionTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode values = Node.fromStrings(
                JsonNameTrait.ID.toString(),
                XmlNameTrait.ID.toString());
        Node node = Node.objectNode().withMember("traits", values);
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#protocolDefinition"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ProtocolDefinitionTrait.class));
        ProtocolDefinitionTrait protocolDefinitionTrait = (ProtocolDefinitionTrait) trait.get();
        assertThat(protocolDefinitionTrait.getTraits(),
                containsInAnyOrder(
                        JsonNameTrait.ID,
                        XmlNameTrait.ID));
        assertThat(protocolDefinitionTrait.toNode(), equalTo(node));
        assertThat(protocolDefinitionTrait.toBuilder().build(), equalTo(protocolDefinitionTrait));
    }
}
