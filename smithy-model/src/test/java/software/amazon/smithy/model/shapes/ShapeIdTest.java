/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class ShapeIdTest {

    @Test
    public void computesHash() {
        assertNotEquals(0, ShapeId.from("name.space#Name").hashCode());
    }

    @Test
    public void returnsWithNewMember() {
        ShapeId a = ShapeId.from("ns.foo#Bar$baz");
        ShapeId b = a.withMember("bam");
        assertEquals("ns.foo#Bar$bam", b.toString());
    }

    @Test
    public void validatesMemberNameSyntax() {
        Assertions.assertThrows(ShapeIdSyntaxException.class, () -> {
            ShapeId.from("ns.foo#Bar").withMember("1invalid");
        });
    }

    @Test
    public void convertsToRelativeRef() {
        ShapeId a = ShapeId.from("ns.foo#Bar$baz");
        assertEquals("Bar$baz", a.asRelativeReference());
    }

    @Test
    public void fromAbsolute() {
        ShapeId id = ShapeId.from("foo.bar#Name$member");
        assertEquals("foo.bar", id.getNamespace());
        assertEquals("Name", id.getName());
        assertEquals("member", id.getMember().get());
    }

    @Test
    public void fromAbsoluteWithoutName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShapeId.from("name.space#"));
    }

    @Test
    public void fromAbsoluteWithoutNamespace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShapeId.from("Name$member"));
    }

    @Test
    public void fromAbsoluteWithDoubleNamespace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ShapeId.from("ns.foo#bar#Name"));
    }

    @Test
    public void fromRelative() {
        ShapeId id = ShapeId.fromRelative("name.space", "Name$member");
        assertEquals("name.space", id.getNamespace());
        assertEquals("Name", id.getName());
        assertEquals("member", id.getMember().get());
    }

    @Test
    public void fromRelativeContainingNamespace() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ShapeId.fromRelative("name.space", "name.space#Name$member");
        });
    }

    @Test
    public void fromOptionalNamespaceTestWithoutNamespace() {
        ShapeId id = ShapeId.fromOptionalNamespace("default.ns", "RelativeName");
        assertEquals("default.ns", id.getNamespace());
        assertEquals("RelativeName", id.getName());
        assertFalse(id.getMember().isPresent());
    }

    @Test
    public void fromOptionalNamespaceTestWithNamespace() {
        ShapeId id = ShapeId.fromOptionalNamespace("default.ns", "other.ns#Name");
        assertEquals("other.ns", id.getNamespace());
        assertEquals("Name", id.getName());
        assertFalse(id.getMember().isPresent());
    }

    @Test
    public void checksIfValidNamespaceName() {
        assertFalse(ShapeId.isValidNamespace("foo.bar."));
        assertFalse(ShapeId.isValidNamespace(".foo.bar"));
        assertFalse(ShapeId.isValidNamespace("foo.bar#baz"));
        assertFalse(ShapeId.isValidNamespace("1foo.bar"));
        assertFalse(ShapeId.isValidNamespace("foo.1bar"));
        assertFalse(ShapeId.isValidNamespace("foo.bar.1"));
        assertFalse(ShapeId.isValidNamespace("foo.bar.*"));
        assertFalse(ShapeId.isValidNamespace("foo.bar.."));
        assertFalse(ShapeId.isValidNamespace(""));

        assertTrue(ShapeId.isValidNamespace("foo"));
        assertTrue(ShapeId.isValidNamespace("Foo.bar"));
        assertTrue(ShapeId.isValidNamespace("foo._bar"));
        assertTrue(ShapeId.isValidNamespace("foo.bar"));
        assertTrue(ShapeId.isValidNamespace("foo.bar1"));
        assertTrue(ShapeId.isValidNamespace("_foo.bar"));
        assertTrue(ShapeId.isValidNamespace("f.b"));
        assertTrue(ShapeId.isValidNamespace("f.b1.c_d"));
        assertTrue(ShapeId.isValidNamespace("f.b1.c_d_.e"));
        assertTrue(ShapeId.isValidNamespace("f.b1.c_d_.e"));
        assertTrue(ShapeId.isValidNamespace("f.b1.c_d_1234.e"));
    }

    @Test
    public void checksIfValidNamespaceNameIdentifierOnly() {
        // Empty String
        assertFalse(ShapeId.isValidNamespace(""));

        // Underscore Only
        assertFalse(ShapeId.isValidNamespace("_"));
        assertFalse(ShapeId.isValidNamespace("__"));

        // Starts with DIGIT
        assertFalse(ShapeId.isValidNamespace("1"));
        assertFalse(ShapeId.isValidNamespace("1_"));
        assertFalse(ShapeId.isValidNamespace("1a"));
        assertFalse(ShapeId.isValidNamespace("12"));

        // IdentifierStart: 1*"_" (ALPHA / DIGIT)
        assertTrue(ShapeId.isValidNamespace("_a"));
        assertTrue(ShapeId.isValidNamespace("_1"));
        assertTrue(ShapeId.isValidNamespace("__a"));
        assertTrue(ShapeId.isValidNamespace("__1"));
        assertFalse(ShapeId.isValidNamespace("__#"));

        // IdentifierStart: ALPHA
        assertTrue(ShapeId.isValidNamespace("a"));
        assertFalse(ShapeId.isValidNamespace("#"));

        // Identifier: 1*"_" (ALPHA / DIGIT) *`IdentifierChars`
        assertTrue(ShapeId.isValidNamespace("_ab"));
        assertTrue(ShapeId.isValidNamespace("_a1"));
        assertTrue(ShapeId.isValidNamespace("_a_"));
        assertFalse(ShapeId.isValidNamespace("_a#"));
        assertTrue(ShapeId.isValidNamespace("_1a"));
        assertTrue(ShapeId.isValidNamespace("_12"));
        assertTrue(ShapeId.isValidNamespace("_1_"));
        assertFalse(ShapeId.isValidNamespace("_1#"));
        assertTrue(ShapeId.isValidNamespace("__ab"));
        assertTrue(ShapeId.isValidNamespace("__a1"));
        assertTrue(ShapeId.isValidNamespace("__a_"));
        assertFalse(ShapeId.isValidNamespace("__a#"));
        assertTrue(ShapeId.isValidNamespace("__1a"));
        assertTrue(ShapeId.isValidNamespace("__12"));
        assertTrue(ShapeId.isValidNamespace("__1_"));
        assertFalse(ShapeId.isValidNamespace("__1#"));

        // Identifier: ALPHA *`IdentifierChars`
        assertTrue(ShapeId.isValidNamespace("ab"));
        assertTrue(ShapeId.isValidNamespace("a1"));
        assertTrue(ShapeId.isValidNamespace("a_"));
        assertFalse(ShapeId.isValidNamespace("a#"));
    }

    @ParameterizedTest
    @MethodSource("shapeIdData")
    public void ShapeIdValidationsTest(String shapeId, boolean isInvalid) {
        try {
            ShapeId id = ShapeId.from(shapeId);
            if (isInvalid) {
                Assertions.fail("Expected ShapeIdSyntaxException for but nothing was raised! " + id);
            }
        } catch (ShapeIdSyntaxException ex) {
            if (!isInvalid) {
                Assertions.fail(String.format("Received unexpected validation error: %s", ex.getMessage()));
            }
        }
    }

    public static Collection<Object[]> shapeIdData() {
        return Arrays.asList(new Object[][] {

                // valid namespaces
                {"n.s#name", false},
                {"name.space#name", false},
                {"name1.space2#name", false},
                {"na.me.spa.ce#name", false},
                {"na.me.spa.ce_#name", false},
                {"na.me.spa.ce__#name", false},
                {"na.me.spa.ce__2#name", false},
                {"na.me.spa.ce__2_#name", false},
                {"namespace#name", false},
                {"mixed.Case#name", false},
                {"NAMESPACE#name", false},
                {"nameSpace#name", false},
                {"Mixed.case#name", false},
                {"_foo.baz#name", false},
                {"__foo.baz#name", false},

                // invalid namespaces
                {"#name", true},
                {"name.space.#name", true},
                {"name..space#name", true},
                {".name.space#name", true},
                {"name-space#name", true},
                {"1namespace.foo#name", true},
                {"a._.b#name", true},
                {"a.____.b#name", true},

                // valid shape names
                {"ns.foo#shapename", false},
                {"ns.foo#shapeName", false},
                {"ns.foo#ShapeName", false},
                {"ns.foo#SHAPENAME", false},
                {"ns.foo#name1", false},
                {"ns.foo#Shape_Name", false},
                {"ns.foo#shape_name_num1", false},
                {"ns.foo#shape_1name", false},
                {"ns.foo#_Shape_Name", false},
                {"ns.foo#__shape_name_num1", false},

                // invalid shape names
                {"ns.foo#", true},
                {"ns.foo#1name", true},
                {"ns.foo#1", true},
                {"ns.foo#shape.name", true},

                // valid segments
                {"ns.foo#name$abc", false},
                {"ns.foo#name$Abc", false},
                {"ns.foo#name$ABC", false},
                {"ns.foo#name$abcMno", false},
                {"ns.foo#name$AbcMno", false},
                {"ns.foo#name$AbcMno1", false},
                {"ns.foo#name$abc_mno", false},
                {"ns.foo#name$Abc_Mno", false},
                {"ns.foo#name$abc_", false},
                {"ns.foo#name$abc__mno", false},
                {"ns.foo#name$_abc", false},
                {"ns.foo#name$_abc_", false},

                // invalid segments
                {"ns.foo#name$", true},
                {"ns.foo#name$abc-mno", true},
                {"ns.foo#name$1abc", true},
                {"ns.foo#name$abc.mno", true},
                {"ns.foo#name$abc.", true},
        });
    }

    @ParameterizedTest
    @MethodSource("toStringData")
    public void toStringTest(
            final String namespace,
            final String name,
            final String member,
            final String expectedResult
    ) {
        ShapeId shapeId = ShapeId.fromParts(namespace, name, member);

        assertEquals(expectedResult, shapeId.toString());
    }

    public static Collection<Object[]> toStringData() {
        return Arrays.asList(new Object[][] {
                {"name.space", "Name", null, "name.space#Name"},
                {"name.space", "Name", "member", "name.space#Name$member"},
        });
    }

    @Test
    public void compareToTest() {
        List<ShapeId> given = Arrays.asList(
                ShapeId.fromParts("ns.foo", "foo"),
                ShapeId.fromParts("ns.foo", "Foo"),
                ShapeId.fromParts("ns.foo", "bar"),
                ShapeId.fromParts("ns.foo", "bar", "member"),
                ShapeId.fromParts("ns.foo", "bar", "Member"),
                ShapeId.fromParts("ns.foo", "bar", "AMember"),
                ShapeId.fromParts("ns.Foo", "foo"),
                ShapeId.fromParts("ns.baz", "foo"));
        given.sort(ShapeId::compareTo);

        List<ShapeId> expected = Arrays.asList(
                ShapeId.fromParts("ns.baz", "foo"),
                ShapeId.fromParts("ns.foo", "bar"),
                ShapeId.fromParts("ns.foo", "bar", "AMember"),
                ShapeId.fromParts("ns.foo", "bar", "Member"),
                ShapeId.fromParts("ns.foo", "bar", "member"),
                ShapeId.fromParts("ns.Foo", "foo"),
                ShapeId.fromParts("ns.foo", "Foo"),
                ShapeId.fromParts("ns.foo", "foo"));

        assertEquals(expected, given);
    }

    @ParameterizedTest
    @MethodSource("equalsData")
    public void equalsTest(final ShapeId lhs, final Object rhs, final boolean expected) {
        assertEquals(lhs.equals(rhs), expected);
    }

    public static Collection<Object[]> equalsData() {

        ShapeId obj = ShapeId.fromParts("ns.foo", "name");
        return Arrays.asList(new Object[][] {
                {obj, obj, true},
                {ShapeId.fromParts("ns.foo", "name"), "other-object", false},
                {ShapeId.fromParts("ns.foo", "name"), ShapeId.fromParts("ns.foo", "name"), true},
                {ShapeId.fromParts("ns.foo", "name1"), ShapeId.fromParts("ns.foo", "name2"), false},
                {ShapeId.fromParts("ns.foo1", "name1"), ShapeId.fromParts("ns.foo2", "name2"), false},
                {ShapeId.fromParts("ns.foo", "n", "a"), ShapeId.fromParts("ns.foo", "n", "a"), true},
                {ShapeId.fromParts("ns.foo", "n", "a"), ShapeId.fromParts("ns.foo", "n", "c"), false},
        });
    }

    @Test
    public void replacesNamespace() {
        ShapeId id = ShapeId.from("foo.baz#Bar$baz");

        assertThat(id.withNamespace("foo.baz"), equalTo(id));
        assertThat(id.withNamespace("foo.bam").toString(), equalTo("foo.bam#Bar$baz"));
    }

    @Test
    public void validatesNamespacesWhenReplaced() {
        Assertions.assertThrows(ShapeIdSyntaxException.class, () -> ShapeId.from("foo#Baz").withNamespace("!"));
    }

    @Test
    public void providesContextualServiceName() {
        ShapeId id = ShapeId.from("foo.bar#Name");
        ServiceShape serviceShape = ServiceShape.builder()
                .id("smithy.example#Service")
                .version("1")
                .putRename(id, "FooName")
                .build();

        assertThat(id.getName(serviceShape), equalTo("FooName"));
    }
}
