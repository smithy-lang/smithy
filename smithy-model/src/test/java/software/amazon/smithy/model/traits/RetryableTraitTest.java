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

public class RetryableTraitTest {
    @Test
    public void loadsTraitWithDefault() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        SourceLocation sourceLocation = new SourceLocation("fileA");
        Node node = Node.objectNodeBuilder().sourceLocation(sourceLocation).build();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#retryable"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(RetryableTrait.class));
        RetryableTrait retryableTrait = (RetryableTrait) trait.get();
        assertThat(retryableTrait.getSourceLocation(), equalTo(sourceLocation));
        assertFalse(retryableTrait.getThrottling());
        assertThat(retryableTrait.toNode(), equalTo(node));
        assertThat(retryableTrait.toNode().getSourceLocation(), equalTo(sourceLocation));
        assertThat(retryableTrait.toBuilder().build(), equalTo(retryableTrait));
        assertThat(retryableTrait.toBuilder().build().getSourceLocation(), equalTo(sourceLocation));
    }

    @Test
    public void loadsTraitWithThrottling() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Node node = Node.objectNodeBuilder().withMember("throttling", true).build();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#retryable"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(RetryableTrait.class));
        RetryableTrait retryableTrait = (RetryableTrait) trait.get();
        assertTrue(retryableTrait.getThrottling());
        assertThat(retryableTrait.toNode(), equalTo(node));
        assertThat(retryableTrait.toBuilder().build(), equalTo(retryableTrait));
    }
}
