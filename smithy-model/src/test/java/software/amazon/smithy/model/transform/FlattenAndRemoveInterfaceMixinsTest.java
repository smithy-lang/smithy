/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.MixinTrait;

public class FlattenAndRemoveInterfaceMixinsTest {

    @Test
    void interfaceMixinsArePreservedInModel() {
        Model before = Model.assembler()
                .addImport(getClass().getResource("flatten-interface-mixins.smithy"))
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().flattenAndRemoveMixins(before);

        // Interface mixin should still be in model
        assertTrue(result.getShape(ShapeId.from("smithy.example#HasName")).isPresent());
        assertTrue(MixinTrait.isInterfaceMixin(result.expectShape(ShapeId.from("smithy.example#HasName"))));

        // Non-interface mixin should be removed
        assertFalse(result.getShape(ShapeId.from("smithy.example#NormalMixin")).isPresent());
    }

    @Test
    void concreteShapeGetsInterfaceMixinRef() {
        Model before = Model.assembler()
                .addImport(getClass().getResource("flatten-interface-mixins.smithy"))
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().flattenAndRemoveMixins(before);

        var concrete = result.expectShape(ShapeId.from("smithy.example#Concrete"), StructureShape.class);
        // Should have the interface mixin ref
        assertTrue(concrete.getMixins().contains(ShapeId.from("smithy.example#HasName")));
        // Should have flattened members from both mixins
        assertTrue(concrete.getMember("name").isPresent());
        assertTrue(concrete.getMember("age").isPresent());
        assertTrue(concrete.getMember("id").isPresent());
    }

    @Test
    void transitiveInterfaceMixinDiscovery() {
        Model before = Model.assembler()
                .addImport(getClass().getResource("flatten-interface-mixins.smithy"))
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().flattenAndRemoveMixins(before);

        // TransitiveUser uses NonInterfaceChild which extends HasName (interface).
        // NonInterfaceChild should be removed, but TransitiveUser should get HasName ref.
        var transitiveUser = result.expectShape(ShapeId.from("smithy.example#TransitiveUser"), StructureShape.class);
        assertTrue(transitiveUser.getMixins().contains(ShapeId.from("smithy.example#HasName")));
        assertFalse(result.getShape(ShapeId.from("smithy.example#NonInterfaceChild")).isPresent());
        // Members from the removed NonInterfaceChild and its parent HasName should be flattened in
        assertTrue(transitiveUser.getMember("name").isPresent());
        assertTrue(transitiveUser.getMember("extra").isPresent());
        assertTrue(transitiveUser.getMember("userId").isPresent());
    }

    @Test
    void interfaceMixinHierarchyPreserved() {
        Model before = Model.assembler()
                .addImport(getClass().getResource("flatten-interface-mixins.smithy"))
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().flattenAndRemoveMixins(before);

        // HasFullName extends HasName (both interface mixins)
        var hasFullName = result.expectShape(ShapeId.from("smithy.example#HasFullName"), StructureShape.class);
        assertTrue(MixinTrait.isInterfaceMixin(hasFullName));
        // HasFullName should still reference HasName as mixin
        assertTrue(hasFullName.getMixins().contains(ShapeId.from("smithy.example#HasName")));
        // HasFullName should have members from HasName flattened in
        assertTrue(hasFullName.getMember("name").isPresent());
        assertTrue(hasFullName.getMember("lastName").isPresent());

        // FullNameUser should implement HasFullName (not HasName directly, since hierarchy handles it)
        var fullNameUser = result.expectShape(ShapeId.from("smithy.example#FullNameUser"), StructureShape.class);
        assertTrue(fullNameUser.getMixins().contains(ShapeId.from("smithy.example#HasFullName")));
    }

    @Test
    void diamondInterfaceMixins() {
        Model before = Model.assembler()
                .addImport(getClass().getResource("flatten-interface-mixins.smithy"))
                .assemble()
                .unwrap();
        Model result = ModelTransformer.create().flattenAndRemoveMixins(before);

        // DiamondUser uses both HasFullName and HasAge (both interface), which both extend HasName
        var diamondUser = result.expectShape(ShapeId.from("smithy.example#DiamondUser"), StructureShape.class);
        Set<ShapeId> mixinRefs = diamondUser.getMixins();
        assertTrue(mixinRefs.contains(ShapeId.from("smithy.example#HasFullName")));
        assertTrue(mixinRefs.contains(ShapeId.from("smithy.example#HasAge")));
        // HasName should NOT appear as a direct ref — it's reachable via HasFullName and HasAge
        assertFalse(mixinRefs.contains(ShapeId.from("smithy.example#HasName")));
    }
}
