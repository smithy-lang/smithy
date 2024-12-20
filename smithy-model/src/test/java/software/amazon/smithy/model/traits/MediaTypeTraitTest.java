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
import software.amazon.smithy.model.shapes.ShapeId;

public class MediaTypeTraitTest {

    @Test
    public void loadsTrait() {
        Node node = Node.from("application/json");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#mediaType"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(MediaTypeTrait.class));
        MediaTypeTrait mediaTypeTrait = (MediaTypeTrait) trait.get();
        assertThat(mediaTypeTrait.getValue(), equalTo("application/json"));
        assertThat(mediaTypeTrait.toNode(), equalTo(node));
    }
}
