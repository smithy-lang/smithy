/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class LengthTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Map<StringNode, Node> values = new HashMap<>();
        values.put(Node.from("min"), Node.from(1L));
        values.put(Node.from("max"), Node.from(10L));
        Node node = Node.objectNode(values);
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#length"), ShapeId.from("ns.qux#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(LengthTrait.class));
        LengthTrait lengthTrait = (LengthTrait) trait.get();
        assertTrue(lengthTrait.getMin().isPresent());
        assertTrue(lengthTrait.getMax().isPresent());
        assertThat(lengthTrait.getMin().get(), equalTo(1L));
        assertThat(lengthTrait.getMax().get(), equalTo(10L));
        assertThat(lengthTrait.toNode(), equalTo(node));
        assertThat(lengthTrait.toBuilder().build(), equalTo(lengthTrait));
    }

    @Test
    public void requiresOneOfMinOrMax() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            Map<StringNode, Node> values = new HashMap<>();

            provider.createTrait(ShapeId.from("smithy.api#length"), ShapeId.from("ns.qux#foo"), Node.objectNode(values));
        });
    }
}
