/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class NotPropertyTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#notProperty"),
                ShapeId.from("ns.qux#foo"),
                Node.objectNode());

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(NotPropertyTrait.class));
        assertThat(trait.get().toNode(), equalTo(Node.objectNode()));
    }

    @Test
    public void testEmptyConstructor() {
        NotPropertyTrait emptyTrait = new NotPropertyTrait();
        NotPropertyTrait otherTrait = new NotPropertyTrait(Node.objectNodeBuilder().build());
        assertEquals(emptyTrait, otherTrait);
    }
}
