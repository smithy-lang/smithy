/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class PropertyTraitTest {
    @Test
    public void loadsTrait() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Map<StringNode, Node> values = new HashMap<>();
        values.put(Node.from("name"), Node.from("propertyName"));
        ObjectNode objectNode = Node.objectNode(values);
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#property"),
            ShapeId.from("ns.qux#foo"), objectNode);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(PropertyTrait.class));
        PropertyTrait propertyTrait = (PropertyTrait) trait.get();

        assertTrue(propertyTrait.getName().isPresent());
        assertEquals("propertyName", propertyTrait.getName().get());

        assertThat(propertyTrait.toNode(), equalTo(objectNode));
        assertThat(propertyTrait.toBuilder().build(), equalTo(propertyTrait));
    }

    @Test
    public void loadsNoName() {
        TraitFactory provider = TraitFactory.createServiceFactory();
        Map<StringNode, Node> values = new HashMap<>();
        ObjectNode objectNode = Node.objectNode(values);
        Optional<Trait> trait = provider.createTrait(ShapeId.from("smithy.api#property"),
            ShapeId.from("ns.qux#foo"), objectNode);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(PropertyTrait.class));
        PropertyTrait propertyTrait = (PropertyTrait) trait.get();

        assertFalse(propertyTrait.getName().isPresent());

        assertThat(propertyTrait.toNode(), equalTo(objectNode));
        assertThat(propertyTrait.toBuilder().build(), equalTo(propertyTrait));
    }
}
