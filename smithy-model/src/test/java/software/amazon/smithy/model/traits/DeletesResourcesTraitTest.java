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

public class DeletesResourcesTraitTest {
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
                ShapeId.from("smithy.api#deletesResources"),
                target,
                values);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(DeletesResourcesTrait.class));
        DeletesResourcesTrait deletesResources = (DeletesResourcesTrait) trait.get();
        assertThat(deletesResources.getBindings(), hasSize(2));
        assertThat(deletesResources.getBindings().get(0).getResource(),
                equalTo(ShapeId.from("ns.example#Foo")));
        assertThat(deletesResources.getBindings().get(0).getIdentifiers().get("fooId").getPath(),
                equalTo("input.fooId"));
        assertThat(deletesResources.getBindings().get(1).getResource(),
                equalTo(ShapeId.from("ns.example#Bar")));
        assertTrue(deletesResources.getBindings().get(1).getIdentifiers().isEmpty());
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
        DeletesResourcesTrait trait = (DeletesResourcesTrait) new DeletesResourcesTrait.Provider()
                .createTrait(target, values);

        assertThat(trait.toNode(), equalTo(values));
        assertThat(trait.toBuilder().build(), equalTo(trait));
    }
}
