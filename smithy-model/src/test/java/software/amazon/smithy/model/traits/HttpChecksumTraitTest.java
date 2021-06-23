/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.HttpChecksumProperty.Location;

public class HttpChecksumTraitTest {

    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();

        List<Node> requestNodes = new ArrayList<>();
        requestNodes.add(Node.objectNode()
                .withMember("in", Location.HEADER.toNode())
                .withMember("name", Node.from("x-checksum-header"))
                .withMember("algorithm", Node.from("crc32")));

        List<Node> responseNodes = new ArrayList<>();
        responseNodes.add(Node.objectNode()
                .withMember("in", Location.HEADER.toNode())
                .withMember("name", Node.from("x-checksum-header"))
                .withMember("algorithm", Node.from("crc32")));

        ObjectNode node = Node.objectNode()
                .withMember("request", ArrayNode.fromNodes(requestNodes))
                .withMember("response", ArrayNode.fromNodes(responseNodes));

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#httpChecksum"), ShapeId.from("ns.qux#foo"), node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(HttpChecksumTrait.class));
        HttpChecksumTrait checksumTrait = (HttpChecksumTrait) trait.get();

        for (HttpChecksumProperty property: checksumTrait.getRequestProperties()) {
            assertThat(property.getLocation(), equalTo(Location.HEADER));
            assertThat(property.getName(), equalTo("x-checksum-header"));
            assertThat(property.getAlgorithm(), equalTo("crc32"));
        }

        for (HttpChecksumProperty property: checksumTrait.getResponseProperties()) {
            assertThat(property.getLocation(), equalTo(Location.HEADER));
            assertThat(property.getName(), equalTo("x-checksum-header"));
            assertThat(property.getAlgorithm(), equalTo("crc32"));
        }

        assertThat(node.expectArrayMember("request"), equalTo(ArrayNode.fromNodes(requestNodes)));
        assertThat(node.expectArrayMember("response"), equalTo(ArrayNode.fromNodes(responseNodes)));
        assertThat(checksumTrait.toNode(), equalTo(node));
        assertThat(checksumTrait.toBuilder().build(), equalTo(checksumTrait));
    }
}
