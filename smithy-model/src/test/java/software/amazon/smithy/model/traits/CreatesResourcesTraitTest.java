/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class CreatesResourcesTraitTest {
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
                                                        .withMember("path", Node.from("output.fooId")))))
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.example#Bar")));
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#createsResources"),
                target,
                values);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(CreatesResourcesTrait.class));
        CreatesResourcesTrait createsResources = (CreatesResourcesTrait) trait.get();
        assertThat(createsResources.getBindings(), hasSize(2));
        assertThat(createsResources.getBindings().get(0).getResource(),
                equalTo(ShapeId.from("ns.example#Foo")));
        assertThat(createsResources.getBindings().get(0).getIdentifiers().get("fooId").getPath(),
                equalTo("output.fooId"));
        assertThat(createsResources.getBindings().get(1).getResource(),
                equalTo(ShapeId.from("ns.example#Bar")));
        assertTrue(createsResources.getBindings().get(1).getIdentifiers().isEmpty());
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
                                                        .withMember("path", Node.from("output.fooId")))));
        CreatesResourcesTrait trait = (CreatesResourcesTrait) new CreatesResourcesTrait.Provider()
                .createTrait(target, values);

        assertThat(trait.toNode(), equalTo(values));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }

    @Test
    public void loadsAndRoundTripsAllBindingMembers() {
        ShapeId target = ShapeId.from("ns.example#MyOperation");
        ArrayNode values = Node.arrayNode()
                .withValue(Node.objectNode()
                        .withMember("resource", Node.from("ns.example#Foo"))
                        .withMember("identifiers",
                                Node.objectNode()
                                        .withMember("fooId",
                                                Node.objectNode()
                                                        .withMember("path", Node.from("result.fooId"))))
                        .withMember("identifiersFrom", Node.from("result"))
                        .withMember("properties",
                                Node.objectNode()
                                        .withMember("size",
                                                Node.objectNode()
                                                        .withMember("path", Node.from("spec.size"))))
                        .withMember("propertiesFrom", Node.from("spec")));
        CreatesResourcesTrait trait = (CreatesResourcesTrait) new CreatesResourcesTrait.Provider()
                .createTrait(target, values);

        ResourceLifecycleBinding binding = trait.getBindings().get(0);
        assertThat(binding.getResource(), equalTo(ShapeId.from("ns.example#Foo")));
        assertThat(binding.getIdentifiers().get("fooId").getPath(), equalTo("result.fooId"));
        assertThat(binding.getIdentifiersFrom(), equalTo(Optional.of("result")));
        assertThat(binding.getProperties().get("size").getPath(), equalTo("spec.size"));
        assertThat(binding.getPropertiesFrom(), equalTo(Optional.of("spec")));

        // Round-trips through node and builder.
        assertThat(trait.toNode(), equalTo(values));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }
}
