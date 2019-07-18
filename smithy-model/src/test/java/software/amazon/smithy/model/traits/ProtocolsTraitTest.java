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

package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class ProtocolsTraitTest {
    @Test
    public void loadsTraits() {
        Node node = Node.arrayNode()
                .withValue(Node.objectNode()
                        .withMember("name", Node.from("foo"))
                        .withMember("auth", ArrayNode.fromStrings(ListUtils.of("foo"))))
                .withValue(Node.objectNode()
                        .withMember("name", Node.from("baz"))
                        .withMember("tags", ArrayNode.fromStrings(ListUtils.of("foo", "bar", "baz")))
                        .withMember("auth", ArrayNode.fromStrings(ListUtils.of("abc", "def"))));
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#protocols"), ShapeId.from("ns.qux#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ProtocolsTrait.class));
        ProtocolsTrait protocolsTrait = (ProtocolsTrait) trait.get();
        assertEquals(protocolsTrait.getProtocols().size(), 2);

        assertTrue(protocolsTrait.getProtocol("foo").isPresent());
        assertThat(protocolsTrait.getAllAuthSchemes(), containsInAnyOrder("abc", "def", "foo"));

        assertThat(protocolsTrait.toNode(), equalTo(node));
        assertThat(protocolsTrait.toBuilder().build(), equalTo(protocolsTrait));
    }
}
