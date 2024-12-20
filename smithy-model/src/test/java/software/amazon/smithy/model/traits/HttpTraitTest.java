/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class HttpTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode node = Node.objectNode()
                .withMember("method", Node.from("PUT"))
                .withMember("uri", Node.from("/foo.baz"))
                .withMember("code", Node.from(200));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#http"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(HttpTrait.class));
        HttpTrait http = (HttpTrait) trait.get();

        assertThat(http.getMethod(), equalTo("PUT"));
        assertThat(http.getUri().toString(), equalTo("/foo.baz"));
        assertThat(http.getCode(), equalTo(200));
        assertThat(http.toNode(), equalTo(node));
        assertThat(http.toBuilder().build(), equalTo(http));
    }
}
