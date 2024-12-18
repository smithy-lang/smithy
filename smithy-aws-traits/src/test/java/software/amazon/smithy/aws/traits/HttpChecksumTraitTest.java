/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInRelativeOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class HttpChecksumTraitTest {

    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();

        List<String> algorithms = new ArrayList<>(Arrays.asList("CRC64NVME", "CRC32C", "CRC32", "SHA1", "SHA256"));
        List<Node> responseAlgorithmNodes = new ArrayList<>();
        for (String algorithm : algorithms) {
            responseAlgorithmNodes.add(Node.from(algorithm));
        }

        ObjectNode node = Node.objectNode()
                .withMember("requestChecksumRequired", Node.from(true))
                .withMember("requestAlgorithmMember", Node.from("ChecksumAlgorithm"))
                .withMember("requestValidationModeMember", Node.from("ChecksumMode"))
                .withMember("responseAlgorithms", ArrayNode.fromNodes(responseAlgorithmNodes));

        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("aws.protocols#httpChecksum"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(HttpChecksumTrait.class));
        HttpChecksumTrait checksumTrait = (HttpChecksumTrait) trait.get();

        assertThat(checksumTrait.isRequestChecksumRequired(), is(true));
        assertThat(checksumTrait.getRequestAlgorithmMember().get(), equalTo("ChecksumAlgorithm"));
        assertThat(checksumTrait.getRequestValidationModeMember().get(), equalTo("ChecksumMode"));
        assertThat(checksumTrait.getResponseAlgorithms(),
                containsInRelativeOrder("CRC64NVME",
                        "CRC32C",
                        "CRC32",
                        "SHA1",
                        "SHA256"));

        assertThat(node.expectBooleanMember("requestChecksumRequired"), equalTo(BooleanNode.from(true)));
        assertThat(node.expectStringMember("requestAlgorithmMember"), equalTo(Node.from("ChecksumAlgorithm")));
        assertThat(node.expectStringMember("requestValidationModeMember"), equalTo(Node.from("ChecksumMode")));
        assertThat(node.expectArrayMember("responseAlgorithms"), equalTo(ArrayNode.fromNodes(responseAlgorithmNodes)));
        assertThat(checksumTrait.toNode(), equalTo(node));
        assertThat(checksumTrait.toBuilder().build(), equalTo(checksumTrait));
    }

    @Test
    public void normalizesAlgorithmName() {
        assertThat(HttpChecksumTrait.getChecksumLocationName("CRC32C"), equalTo("x-amz-checksum-crc32c"));
    }
}
