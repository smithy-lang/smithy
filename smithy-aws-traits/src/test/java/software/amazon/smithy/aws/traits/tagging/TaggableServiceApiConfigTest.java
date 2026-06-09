/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class TaggableServiceApiConfigTest {
    @Test
    public void roundTripsAllMembers() {
        TaggableServiceApiConfig cfg = TaggableServiceApiConfig.builder()
                .tagApi(ShapeId.from("ns.qux#TagIt"))
                .untagApi(ShapeId.from("ns.qux#Untag"))
                .listTagsApi(ShapeId.from("ns.qux#ListTags"))
                .build();

        Node node = cfg.toNode();
        assertThat(node,
                equalTo(Node.objectNodeBuilder()
                        .withMember("tagApi", "ns.qux#TagIt")
                        .withMember("untagApi", "ns.qux#Untag")
                        .withMember("listTagsApi", "ns.qux#ListTags")
                        .build()));
        assertThat(cfg.toBuilder().build(), equalTo(cfg));
    }

    @Test
    public void allMembersOptional() {
        TaggableServiceApiConfig cfg = TaggableServiceApiConfig.builder().build();
        assertFalse(cfg.getTagApi().isPresent());
        assertFalse(cfg.getUntagApi().isPresent());
        assertFalse(cfg.getListTagsApi().isPresent());
        assertThat(cfg.toNode(), equalTo(Node.objectNode()));
    }

    @Test
    public void supportsPartialConfiguration() {
        TaggableServiceApiConfig cfg = TaggableServiceApiConfig.builder()
                .tagApi(ShapeId.from("ns.qux#TagIt"))
                .build();

        assertEquals(Optional.of(ShapeId.from("ns.qux#TagIt")), cfg.getTagApi());
        assertFalse(cfg.getUntagApi().isPresent());
        assertFalse(cfg.getListTagsApi().isPresent());

        Node node = cfg.toNode();
        assertThat(node,
                equalTo(Node.objectNodeBuilder()
                        .withMember("tagApi", "ns.qux#TagIt")
                        .build()));
    }
}
