/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;

public class EnumTraitTest {
    @Test
    public void loadsTrait() {
        Node node = Node.parse("[{\"value\": \"foo\"}, "
                + "{\"value\": \"bam\"}, "
                + "{\"value\": \"boozled\"}]");
        EnumTrait trait = new EnumTrait.Provider().createTrait(ShapeId.from("ns.foo#baz"), node);

        assertThat(trait.toNode(), equalTo(node));
        assertThat(trait.toBuilder().build(), equalTo(trait));
        assertThat(trait.getEnumDefinitionValues(), contains("foo", "bam", "boozled"));
    }

    @Test
    public void expectsAtLeastOneConstant() {
        Assertions.assertThrows(SourceException.class, () -> {
            TraitFactory provider = TraitFactory.createServiceFactory();
            provider.createTrait(ShapeId.from("smithy.api#enum"), ShapeId.from("ns.qux#foo"), Node.objectNode());
        });
    }

    @Test
    public void checksIfAllDefineNames() {
        Node node = Node.parse("[{\"value\": \"foo\", \"name\": \"FOO\"}, "
                + "{\"value\": \"bam\", \"name\": \"BAM\"}]");
        EnumTrait trait = new EnumTrait.Provider().createTrait(ShapeId.from("ns.foo#baz"), node);

        assertThat(trait.hasNames(), is(true));
    }
}
