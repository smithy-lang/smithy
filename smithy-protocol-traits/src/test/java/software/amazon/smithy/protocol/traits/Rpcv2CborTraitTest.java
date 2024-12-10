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

public class Rpcv2CborTraitTest {

    @Test
    public void loadsTraitWithDefaults() {
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(Rpcv2CborTrait.ID, ShapeId.from("ns.foo#foo"), node);

        Assertions.assertTrue(trait.isPresent());
        Assertions.assertTrue(trait.get() instanceof Rpcv2CborTrait);
        Rpcv2CborTrait smithyRpcV2Trait = (Rpcv2CborTrait) trait.get();
        Assertions.assertEquals(smithyRpcV2Trait.toNode(), node);
    }
}
