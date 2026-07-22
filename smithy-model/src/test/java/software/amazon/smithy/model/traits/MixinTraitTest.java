/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

public class MixinTraitTest {

    @Test
    public void loadsEmptyTrait() {
        Node node = Node.objectNode();
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(
                ShapeId.from("smithy.api#mixin"),
                ShapeId.from("ns.qux#foo"),
                node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(MixinTrait.class));
        MixinTrait mixinTrait = (MixinTrait) trait.get();

        // Returns MixinTrait.ID by default.
        assertThat(mixinTrait.getLocalTraits(), contains(MixinTrait.ID));

        // But doesn't serialize it since it's implied.
        assertThat(mixinTrait.toNode(), equalTo(node));

        // Interface defaults to false
        assertFalse(mixinTrait.isInterface());
    }

    @Test
    public void retainsSourceLocation() {
        SourceLocation source = new SourceLocation("/foo", 0, 0);
        MixinTrait trait = MixinTrait.builder().sourceLocation(source).build();
        MixinTrait rebuilt = trait.toBuilder().build();

        assertThat(trait.getSourceLocation(), equalTo(rebuilt.getSourceLocation()));
    }

    @Test
    public void retainsMixinLocalTraits() {
        MixinTrait trait = MixinTrait.builder().addLocalTrait(SensitiveTrait.ID).build();
        MixinTrait rebuilt = trait.toBuilder().build();
        assertThat(trait.getLocalTraits(), hasItem(SensitiveTrait.ID));
        assertThat(trait.getLocalTraits(), equalTo(rebuilt.getLocalTraits()));

        assertThat(rebuilt, equalTo(trait));
    }

    @Test
    public void isInterfaceRoundTripsViaSerde() {
        MixinTrait trait = MixinTrait.builder().isInterface(true).build();
        Node node = trait.toNode();

        // Verify the node has the interface member set to true
        assertTrue(node.expectObjectNode().getMember("interface").isPresent());
        assertTrue(node.expectObjectNode().getMember("interface").get().expectBooleanNode().getValue());

        // Verify it can be deserialized back
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> deserialized = provider.createTrait(
                ShapeId.from("smithy.api#mixin"),
                ShapeId.from("ns.qux#foo"),
                node);
        assertTrue(deserialized.isPresent());
        MixinTrait deserializedMixin = (MixinTrait) deserialized.get();
        assertTrue(deserializedMixin.isInterface());
    }

    @Test
    public void isInterfaceFalseDoesNotSerialize() {
        MixinTrait trait = MixinTrait.builder().isInterface(false).build();
        Node node = trait.toNode();

        // When interface is false, it should not appear in serialized form
        assertFalse(node.expectObjectNode().getMember("interface").isPresent());
    }

    @Test
    public void isInterfaceMixinStaticHelper() {
        MixinTrait interfaceMixin = MixinTrait.builder().isInterface(true).build();
        MixinTrait normalMixin = MixinTrait.builder().build();

        StructureShape ifaceShape = StructureShape.builder()
                .id("ns.foo#IfaceMixin")
                .addTrait(interfaceMixin)
                .build();
        StructureShape normalShape = StructureShape.builder()
                .id("ns.foo#NormalMixin")
                .addTrait(normalMixin)
                .build();
        StructureShape noMixin = StructureShape.builder()
                .id("ns.foo#NoMixin")
                .build();

        assertTrue(MixinTrait.isInterfaceMixin(ifaceShape));
        assertFalse(MixinTrait.isInterfaceMixin(normalShape));
        assertFalse(MixinTrait.isInterfaceMixin(noMixin));
    }
}
