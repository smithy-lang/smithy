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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class XmlAttributeTraitTest {
    @Test
    public void loadsTraitWithString() {
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#xmlAttribute"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(XmlAttributeTrait.class));
        XmlAttributeTrait xmlAttributeTrait = (XmlAttributeTrait) trait.get();
        assertThat(xmlAttributeTrait.toNode(), equalTo(node));
    }

    @Test
    public void validatesInput() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            provider.createTrait(ShapeId.from("smithy.api#xmlAttribute"), ShapeId.from("ns.qux#foo"), Node.from("abc"));
        });
    }
}
