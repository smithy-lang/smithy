/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except
 * in compliance with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package software.amazon.smithy.protocols.traits;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;
import java.util.Optional;

public class Rpcv2TraitTest {

    @Test
    public void loadsTraitWithDefaults() {
        Node node = Node.objectNode().withMember("format", Node.fromStrings("cors"));
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait =
                provider.createTrait(Rpcv2Trait.ID, ShapeId.from("ns.foo#foo"), node);

        Assertions.assertTrue(trait.isPresent());
        Assertions.assertTrue(trait.get() instanceof Rpcv2Trait);
        Rpcv2Trait smithyRpcV2Trait = (Rpcv2Trait) trait.get();
        Assertions.assertEquals(smithyRpcV2Trait.toNode(), node);
    }
}
