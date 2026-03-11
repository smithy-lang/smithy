/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocol.traits;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class Rpcv2JsonTraitTest {

    @Test
    public void loadsTraitWithDefaults() {
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(Rpcv2JsonTrait.ID, ShapeId.from("ns.foo#foo"), node);

        Assertions.assertTrue(trait.isPresent());
        Assertions.assertTrue(trait.get() instanceof Rpcv2JsonTrait);
        Rpcv2JsonTrait smithyRpcV2Trait = (Rpcv2JsonTrait) trait.get();
        Assertions.assertEquals(smithyRpcV2Trait.toNode(), node);
    }
}
