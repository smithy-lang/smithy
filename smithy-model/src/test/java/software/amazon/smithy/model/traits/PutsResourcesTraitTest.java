/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class PutsResourcesTraitTest {
    @Test
    public void loadsTrait() {
        ShapeId target = ShapeId.from("ns.example#MyOperation");
        TraitFactory provider = TraitFactory.createServiceFactory();
        ArrayNode values = Node.arrayNode()
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.example#Foo"))
                        .withMember("identifiers",
                                Node.objectNode()
                                        .withMember("fooId",
                                                Node.objectNode()
                                                        .withMember("path", Node.from("input.fooId")))))
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.example#Bar")));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#putsResources"),
                target,
                values);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(PutsResourcesTrait.class));
        PutsResourcesTrait putsResources = (PutsResourcesTrait) trait.get();
        assertThat(putsResources.getBindings(), hasSize(2));
        assertThat(putsResources.getBindings().get(0).getResource(),
                equalTo(ShapeId.from("ns.example#Foo")));
        assertThat(putsResources.getBindings().get(0).getIdentifiers().get("fooId").getPath(),
                equalTo("input.fooId"));
        assertThat(putsResources.getBindings().get(1).getResource(),
                equalTo(ShapeId.from("ns.example#Bar")));
        assertTrue(putsResources.getBindings().get(1).getIdentifiers().isEmpty());
    }

    @Test
    public void convertsToNodeAndBuilder() {
        ShapeId target = ShapeId.from("ns.example#MyOperation");
        ArrayNode values = Node.arrayNode()
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.example#Foo"))
                        .withMember("identifiers",
                                Node.objectNode()
                                        .withMember("fooId",
                                                Node.objectNode()
                                                        .withMember("path", Node.from("input.fooId")))));
        PutsResourcesTrait trait = (PutsResourcesTrait) new PutsResourcesTrait.Provider()
                .createTrait(target, values);

        assertThat(trait.toNode(), equalTo(values));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void loadsAndConvertsNoReplace() {
        ShapeId target = ShapeId.from("ns.example#MyOperation");
        ArrayNode values = Node.arrayNode()
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.example#Foo"))
                        .withMember("noReplace", Node.from(true)))
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.example#Bar")));
        PutsResourcesTrait trait = (PutsResourcesTrait) new PutsResourcesTrait.Provider()
                .createTrait(target, values);

        assertTrue(trait.getBindings().get(0).isNoReplace());
        assertFalse(trait.getBindings().get(1).isNoReplace());

        // Rebuild the trait so toNode serializes the bindings instead of returning the parsed node.
        PutsResourcesTrait rebuilt = PutsResourcesTrait.builder().bindings(trait.getBindings()).build();
        assertThat(rebuilt.toNode(), equalTo(values));

        // Trait equality compares nodes, so check the bindings themselves round-trip noReplace.
        for (ResourcePutBinding binding : trait.getBindings()) {
            assertThat(binding.toBuilder().build(), equalTo(binding));
        }
        assertThat(trait.getBindings().get(0),
                not(equalTo(trait.getBindings().get(0).toBuilder().noReplace(false).build())));
    }
}
