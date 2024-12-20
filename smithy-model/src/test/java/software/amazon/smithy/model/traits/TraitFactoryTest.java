/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

public class TraitFactoryTest {
    @Test
    public void returnsEmptyIfNoCustomTraitDefined() {
        TraitFactory factory = TraitFactory.createServiceFactory(ListUtils.of());
        assertFalse(factory.createTrait(ShapeId.from("ns.foo#baz"), ShapeId.from("ns.qux#foo"), Node.objectNode())
                .isPresent());
    }
}
