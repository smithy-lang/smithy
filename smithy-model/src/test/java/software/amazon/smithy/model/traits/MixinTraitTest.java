/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class MixinTraitTest {

    @Test
    public void loadsEmptyTrait() {
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#mixin"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(MixinTrait.class));
        MixinTrait mixinTrait = (MixinTrait) trait.get();

        // Returns MixinTrait.ID by default.
        assertThat(mixinTrait.getLocalTraits(), contains(MixinTrait.ID));

        // But doesn't serialize it since it's implied.
        assertThat(mixinTrait.toNode(), equalTo(node));
    }

    @Test
    public void retainsSourceLocation() {
        SourceLocation source = new SourceLocation("/foo", 0, 0);
        MixinTrait trait = MixinTrait.builder().sourceLocation(source).build();
        MixinTrait rebuilt = trait.toBuilder().build();

        assertThat(trait.getSourceLocation(), equalTo(rebuilt.getSourceLocation()));
    }

    @Test
    public void retainsMixinLocalTraits() {
        MixinTrait trait = MixinTrait.builder().addLocalTrait(SensitiveTrait.ID).build();
        MixinTrait rebuilt = trait.toBuilder().build();
        assertThat(trait.getLocalTraits(), hasItem(SensitiveTrait.ID));
        assertThat(trait.getLocalTraits(), equalTo(rebuilt.getLocalTraits()));

        assertThat(rebuilt, equalTo(trait));
    }
}
