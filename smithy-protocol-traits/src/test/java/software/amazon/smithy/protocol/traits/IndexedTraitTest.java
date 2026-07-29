/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class IndexedTraitTest {

    @Test
    public void loadsAndRoundTripsTrait() {
        SourceLocation location = new SourceLocation("example.smithy", 3, 1);
        Node node = Node.objectNodeBuilder().sourceLocation(location).build();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                IndexedTrait.ID,
                ShapeId.from("smithy.example#Example"),
                node);

        Assertions.assertTrue(trait.isPresent());
        Assertions.assertInstanceOf(IndexedTrait.class, trait.get());
        Assertions.assertEquals(location, trait.get().getSourceLocation());
        Assertions.assertSame(node, trait.get().toNode());
    }
}
