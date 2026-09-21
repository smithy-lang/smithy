/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.ShapeId;

public class SyntheticShapeNamingTest {

    private static final String NS = "smithy.example";

    @Test
    public void listNameWithPreludeTargetUsesDoubleUnderscore() {
        assertEquals("_SyntheticListOf__String",
                LoaderUtils.listName(NS, ShapeId.from("smithy.api#String")));
    }

    @Test
    public void listNameWithSameNamespaceTargetUsesSingleUnderscore() {
        assertEquals("_SyntheticListOf_Widget",
                LoaderUtils.listName(NS, ShapeId.from("smithy.example#Widget")));
    }

    @Test
    public void listNameWithForeignTargetFlattensNamespace() {
        assertEquals("_SyntheticListOf_com_foo_Bar",
                LoaderUtils.listName(NS, ShapeId.from("com.foo#Bar")));
    }

    @Test
    public void mapNameSeparatesKeyAndValueWithTo() {
        assertEquals("_SyntheticMapOf__String_To___Integer",
                LoaderUtils.mapName(NS, ShapeId.from("smithy.api#String"), ShapeId.from("smithy.api#Integer")));
    }

    // The prelude marker (double underscore) keeps a prelude target distinct from a
    // same-namespace target that shares the simple name.
    @Test
    public void preludeAndSameNamespaceWithSameSimpleNameAreDistinct() {
        String prelude = LoaderUtils.listName(NS, ShapeId.from("smithy.api#String"));
        String local = LoaderUtils.listName(NS, ShapeId.from("smithy.example#String"));
        assertNotEquals(prelude, local);
    }

    // A same-namespace target whose simple name starts with `_` switches to the flattened
    // namespace form, so it does not fuse with the prelude marker (`__`).
    @Test
    public void underscorePrefixedNameUsesFlattenedNamespaceForm() {
        String local = LoaderUtils.listName(NS, ShapeId.from("smithy.example#_String"));
        String prelude = LoaderUtils.listName(NS, ShapeId.from("smithy.api#String"));
        assertEquals("_SyntheticListOf_smithy_example__String", local);
        assertNotEquals(local, prelude);
    }

    // A same-namespace synthetic target (a nested inline collection) uses the short
    // same-namespace form rather than the flattened namespace form.
    @Test
    public void nestedSyntheticTargetUsesShortForm() {
        assertEquals("_SyntheticListOf__SyntheticListOf__String",
                LoaderUtils.listName(NS, ShapeId.from("smithy.example#_SyntheticListOf__String")));
    }

    // A same-namespace non-synthetic shape whose name starts with `_` keeps the flattened
    // namespace form so it cannot fuse with the prelude marker.
    @Test
    public void underscorePrefixedNonSyntheticNameUsesFlattenedNamespaceForm() {
        assertEquals("_SyntheticListOf_smithy_example__Foo",
                LoaderUtils.listName(NS, ShapeId.from("smithy.example#_Foo")));
    }

    // Two different targets that share a simple name but live in different namespaces
    // must produce different synthetic names.
    @Test
    public void distinctNamespacesWithSameSimpleNameAreDistinct() {
        String foo = LoaderUtils.listName("audit", ShapeId.from("audit.foo#Widget"));
        String bar = LoaderUtils.listName("audit", ShapeId.from("audit.bar#Widget"));
        assertNotEquals(foo, bar);
    }
}
