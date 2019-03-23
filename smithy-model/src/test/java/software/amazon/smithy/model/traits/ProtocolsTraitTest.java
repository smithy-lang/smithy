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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class ProtocolsTraitTest {
    @Test
    public void loadsTraits() {
        Node node = Node.objectNode()
                .withMember("foo", Node.objectNode())
                .withMember("baz", Node.objectNode()
                        .withMember("deprecated", Node.from(true))
                        .withMember("deprecationReason", Node.from("Just because"))
                        .withMember("tags", ArrayNode.fromStrings(Arrays.asList("foo", "bar", "baz")))
                        .withMember("settings", Node.objectNode()
                                           .withMember("test.property", Node.from("abc"))
                                           .withMember("ns.name", Node.from("def")))
                        .withMember("authentication", ArrayNode.fromStrings(Arrays.asList("abc", "def"))));
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait("smithy.api#protocols", ShapeId.from("ns.qux#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ProtocolsTrait.class));
        ProtocolsTrait protocolsTrait = (ProtocolsTrait) trait.get();
        assertEquals(protocolsTrait.getProtocols().size(), 2);

        Iterator<Map.Entry<String, ProtocolsTrait.Protocol>> iter = protocolsTrait.getProtocols()
                .entrySet().iterator();
        Map.Entry<String, ProtocolsTrait.Protocol> entry = iter.next();
        assertThat(entry.getKey(), equalTo("foo"));

        entry = iter.next();
        assertThat(entry.getKey(), equalTo("baz"));

        assertThat(entry.getValue().getAllSettings(), hasKey("test.property"));
        assertThat(entry.getValue().getAllSettings(), hasKey("ns.name"));

        assertThat(entry.getValue().getAuthentication(), contains("abc", "def"));

        assertThat(protocolsTrait.toNode(), equalTo(node));
        assertThat(protocolsTrait.toBuilder().build(), equalTo(protocolsTrait));
    }
}
