/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class RestXmlTraitTest {

    @Test
    public void loadsTraitWithDefaults() {
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(RestXmlTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(RestXmlTrait.class));
        RestXmlTrait restXmlTrait = (RestXmlTrait) trait.get();
        assertTrue(restXmlTrait.getHttp().isEmpty());
        assertTrue(restXmlTrait.getEventStreamHttp().isEmpty());
        assertFalse(restXmlTrait.isNoErrorWrapping());
        assertThat(restXmlTrait.createNode(), equalTo(node));
    }

    @Test
    public void canSetNoErrorWrapping() {
        Node node = Node.parse("{\"noErrorWrapping\":true}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(RestXmlTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        RestXmlTrait restXmlTrait = (RestXmlTrait) trait.get();
        assertTrue(restXmlTrait.isNoErrorWrapping());
        assertThat(restXmlTrait.createNode(), equalTo(node));
    }
}
