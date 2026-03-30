/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class LongPollTraitTest {
    @Test
    public void loadsTraitWithoutTimeout() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        SourceLocation sourceLocation = new SourceLocation("example.smithy");
        Node node = Node.objectNodeBuilder().sourceLocation(sourceLocation).build();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#longPoll"),
                ShapeId.from("smithy.example#FooOperation"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(LongPollTrait.class));
        LongPollTrait longPollTrait = (LongPollTrait) trait.get();
        assertThat(longPollTrait.getSourceLocation(), equalTo(sourceLocation));
        assertFalse(longPollTrait.getTimeoutMillis().isPresent());
        assertThat(longPollTrait.toNode(), equalTo(node));
        assertThat(longPollTrait.toNode().getSourceLocation(), equalTo(sourceLocation));
        assertThat(longPollTrait.toBuilder().build(), equalTo(longPollTrait));
        assertThat(longPollTrait.toBuilder().build().getSourceLocation(), equalTo(sourceLocation));
    }

    @Test
    public void loadsTraitWithTimeout() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = Node.objectNodeBuilder().withMember("timeoutMillis", 70000).build();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#longPoll"),
                ShapeId.from("smithy.example#FooOperation"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(LongPollTrait.class));
        LongPollTrait longPollTrait = (LongPollTrait) trait.get();
        assertThat(longPollTrait.getTimeoutMillis().get(), equalTo(70000));
        assertThat(longPollTrait.toNode(), equalTo(node));
        assertThat(longPollTrait.toBuilder().build(), equalTo(longPollTrait));
    }
}
