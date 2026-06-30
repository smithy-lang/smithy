/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.traits.unions.UnionTrait;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ListUtils;

/**
 * Verifies the value getters generated for union traits.
 *
 * <p>The type-safe {@code getContents()} getter narrows to the variant's value type via a
 * covariant return, so callers keep the concrete type after an {@code instanceof}
 * check. The legacy {@code getValue()} getter is retained, but deprecated, for
 * backwards compatibility and is exercised separately as a regression test.
 */
public class UnionValueGetterTest {

    @Test
    void valueGetterReturnsConcreteTypeForStringVariant() {
        UnionTrait trait = UnionTrait.builder()
                .stringVariantMember(ShapeId.from("test.abc#myShape"))
                .build();

        // The covariant return lets the value be used at its concrete type after
        // narrowing, with no cast.
        if (trait instanceof UnionTrait.StringVariantMember member) {
            ShapeId value = member.getContents();
            assertEquals(ShapeId.from("test.abc#myShape"), value);
        } else {
            throw new AssertionError("Expected a StringVariantMember but got " + trait.getClass());
        }
    }

    @Test
    void valueGetterReturnsConcreteTypeForListVariant() {
        UnionTrait trait = UnionTrait.builder()
                .listVariantMember(ListUtils.of("1", "2", "3"))
                .build();

        UnionTrait.ListVariantMember member = (UnionTrait.ListVariantMember) trait;
        List<String> value = member.getContents();
        assertEquals(ListUtils.of("1", "2", "3"), value);
    }

    @Test
    void valueGetterReturnsNullForUnitVariant() {
        UnionTrait trait = UnionTrait.builder().unitVariantMember().build();

        assertNull(trait.getContents());
    }

    @Test
    void baseValueGetterReturnsObject() {
        UnionTrait trait = UnionTrait.builder()
                .integerVariantMember(123)
                .build();

        // On the base type the value is exposed as Object, so no false type promise is
        // made to the caller.
        Object value = trait.getContents();
        assertEquals(123, value);
    }

    /**
     * The deprecated, type-erased {@code getValue()} getter still returns the value,
     * but the synthetic cast inserted at the call site throws when the caller assumes
     * the wrong type. This is the unsafe behavior that
     * {@link UnionTrait#getContents()} replaces.
     */
    @Test
    @SuppressWarnings("deprecation")
    void deprecatedGetValueStillWorksButErasesType() {
        UnionTrait trait = UnionTrait.builder()
                .integerVariantMember(123)
                .build();

        Integer correct = trait.getValue();
        assertEquals(123, correct);

        // The unchecked cast inside getValue() is a no-op. The real cast happens at the
        // call site and fails there.
        assertThrows(ClassCastException.class, () -> {
            String wrong = trait.getValue();
        });
    }
}
