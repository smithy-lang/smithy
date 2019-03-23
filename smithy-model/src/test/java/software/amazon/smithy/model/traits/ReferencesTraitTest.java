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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class ReferencesTraitTest {
    @Test
    public void loadsTrait() {
        ShapeId id = ShapeId.from("ns.qux#foo");
        TraitFactory provider = TraitFactory.createServiceFactory();
        ObjectNode values = Node.objectNode()
                .withMember("name1", Node.objectNode()
                        .withMember("resource", Node.from("SomeShape"))
                        .withMember("ids", Node.parse("{\"a\": \"a\", \"b\": \"b\"}")))
                .withMember("name2", Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("rel", Node.from("rel")))
                .withMember("name3", Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("service", Node.from("com.foo#Baz")));
        Optional<Trait> trait = provider.createTrait("smithy.api#references", id, values);
        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ReferencesTrait.class));
        ReferencesTrait referencesTrait = (ReferencesTrait) trait.get();

        assertThat(referencesTrait.getReferences(), hasSize(3));
        assertTrue(referencesTrait.getReference("name1").isPresent());
        assertTrue(referencesTrait.getReference("name2").isPresent());
        assertTrue(referencesTrait.getReference("name3").isPresent());

        assertEquals(referencesTrait.getReference("name1").get().getResource(), ShapeId.from("ns.qux#SomeShape"));
        assertEquals(referencesTrait.getReference("name2").get().getResource(), ShapeId.from(("ns.foo#OtherShape")));
        assertEquals(referencesTrait.getReference("name3").get().getResource(), ShapeId.from(("ns.foo#OtherShape")));

        assertTrue(referencesTrait.getReference("name1").get().getIds().containsKey("a"));
        assertTrue(referencesTrait.getReference("name1").get().getIds().containsKey("b"));
        assertTrue(referencesTrait.getReference("name2").get().getIds().containsKey("c"));
        assertTrue(referencesTrait.getReference("name3").get().getIds().containsKey("c"));

        assertEquals(referencesTrait.getReference("name1").get().getRel(), Optional.empty());
        assertEquals(referencesTrait.getReference("name2").get().getRel(), Optional.of("rel"));
        assertEquals(referencesTrait.getReference("name3").get().getRel(), Optional.empty());

        assertFalse(referencesTrait.getReference("name1").get().getService().isPresent());
        assertFalse(referencesTrait.getReference("name2").get().getService().isPresent());
        assertThat(referencesTrait.getReference("name3").get().getService(),
                   equalTo(Optional.of(ShapeId.from("com.foo#Baz"))));
    }

    @Test
    public void convertsToNodeAndBuilder() {
        ShapeId id = ShapeId.from("ns.qux#foo");
        ObjectNode values = Node.objectNode()
                .withMember("name1", Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#SomeShape"))
                        .withMember("ids", Node.parse("{\"a\": \"a\", \"b\": \"b\"}")))
                .withMember("name2", Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("rel", Node.from("rel")))
                .withMember("name3", Node.objectNode()
                        .withMember("resource", Node.from("ns.foo#OtherShape"))
                        .withMember("ids", Node.parse("{\"c\": \"c\"}"))
                        .withMember("service", Node.from("foo.baz#Bar")));
        ReferencesTrait trait = new ReferencesTrait.Provider().createTrait(id, values);

        assertThat(trait.toNode(), equalTo(values));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }
}
