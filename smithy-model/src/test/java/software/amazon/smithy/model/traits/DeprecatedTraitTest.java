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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;

public class DeprecatedTraitTest {
    @Test
    public void loadsTraitWithString() {
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#deprecated"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(DeprecatedTrait.class));
        DeprecatedTrait deprecatedTrait = (DeprecatedTrait) trait.get();
        assertThat(deprecatedTrait.toNode(), equalTo(node));
    }

    @Test
    public void validatesInput() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            provider.createTrait(ShapeId.from("smithy.api#deprecated"), ShapeId.from("ns.qux#foo"), Node.from("abc"));
        });
    }

    @Test
    public void returnDefaultDescription() {
        DeprecatedTrait deprecatedTrait = DeprecatedTrait.builder().build();
        assertThat(deprecatedTrait.getDeprecatedDescription(ShapeType.OPERATION),
                equalTo("This operation is deprecated."));
        assertThat(deprecatedTrait.getDeprecatedDescription(ShapeType.STRING), equalTo("This shape is deprecated."));
    }

    @Test
    public void returnDescriptionWhenMessageSet() {
        DeprecatedTrait deprecatedTrait = DeprecatedTrait.builder().message("Use X shape instead.").build();
        assertThat(deprecatedTrait.getDeprecatedDescription(ShapeType.STRING),
                equalTo("This shape is deprecated: Use X shape instead."));
    }

    @Test
    public void returnDescriptionWhenSinceSet() {
        DeprecatedTrait deprecatedTrait = DeprecatedTrait.builder().since("2020-01-01").build();
        assertThat(deprecatedTrait.getDeprecatedDescription(ShapeType.STRING),
                equalTo("This shape is deprecated since 2020-01-01."));
    }

    @Test
    public void returnDescriptionWhenBothSinceAndMessageSet() {
        DeprecatedTrait deprecatedTrait =
                DeprecatedTrait.builder().since("2020-01-01").message("Use X shape instead.").build();
        assertThat(deprecatedTrait.getDeprecatedDescription(ShapeType.STRING),
                equalTo("This shape is deprecated since 2020-01-01: Use X shape instead."));
    }
}
