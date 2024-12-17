/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class ServiceProviderTraitFactoryTest {
    @Test
    public void createsTraitsUsingServiceLoader() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        Optional<Trait> maybeTrait = factory.createTrait(
                ShapeId.from("smithy.api#jsonName"),
                ShapeId.from("ns.qux#foo"),
                Node.from("hi"));

        assertTrue(maybeTrait.isPresent());
        assertThat(maybeTrait.get(), instanceOf(JsonNameTrait.class));
    }

    @Test
    public void returnsEmptyWhenNoMatchingTraitIsFound() {
        TraitFactory factory = TraitFactory.createServiceFactory();
        Optional<Trait> maybeTrait = factory.createTrait(
                ShapeId.from("missing.baz#foo"),
                ShapeId.from("ns.qux#foo"),
                Node.nullNode());
        assertFalse(maybeTrait.isPresent());
    }
}
