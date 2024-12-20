/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class XmlNameTraitTest {
    @Test
    public void loadsTraitWithString() {
        Node node = Node.from("Text");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait =
                provider.createTrait(ShapeId.from("smithy.api#xmlName"), ShapeId.from("ns.qux#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(XmlNameTrait.class));
        XmlNameTrait xmlNameTrait = (XmlNameTrait) trait.get();
        assertThat(xmlNameTrait.getValue(), equalTo("Text"));
        assertThat(xmlNameTrait.toNode(), equalTo(node));
    }

    @Test
    public void loadsWithColonInValue() {
        Node node = Node.from("xsi:type");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait =
                provider.createTrait(ShapeId.from("smithy.api#xmlName"), ShapeId.from("ns.qux#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(XmlNameTrait.class));
        XmlNameTrait xmlNameTrait = (XmlNameTrait) trait.get();
        assertThat(xmlNameTrait.getValue(), equalTo("xsi:type"));
        assertThat(xmlNameTrait.toNode(), equalTo(node));
    }
}
