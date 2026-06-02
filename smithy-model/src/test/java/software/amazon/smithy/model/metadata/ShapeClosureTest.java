/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;

public class ShapeClosureTest {

    private static Model model;

    @BeforeAll
    public static void before() {
        model = Model.assembler()
                .addImport(ShapeClosureTest.class.getResource("shape-closure-test.smithy"))
                .assemble()
                .unwrap();
    }

    @Test
    public void readsClosuresFromModelKeyedByIdInOrder() {
        Map<String, ShapeClosure> closures = ShapeClosure.fromModel(model);

        assertThat(closures.keySet(), contains("com.example#primary", "com.example#secondary"));

        ShapeClosure secondary = closures.get("com.example#secondary");
        assertFalse(secondary.getIncludeBySelector().isPresent());
        assertThat(secondary.getRename(), equalTo(Collections.emptyMap()));
    }

    @Test
    public void readsClosureMembersAndRoundTrips() {
        ShapeClosure primary = ShapeClosure.fromModel(model).get("com.example#primary");

        Map<ShapeId, String> expectedRename = new LinkedHashMap<>();
        expectedRename.put(ShapeId.from("com.example#Foo"), "RenamedFoo");

        assertThat(primary.getId(), equalTo("com.example#primary"));
        assertThat(primary.getIncludeNamespaces(), contains("com.example"));
        assertThat(primary.getIncludeBySelector(), equalTo(Optional.of("string")));
        assertThat(primary.getRename(), equalTo(expectedRename));
        assertThat(ShapeClosure.fromNode(primary.toNode()), equalTo(primary));
        assertThat(primary.toBuilder().build(), equalTo(primary));
    }

    @Test
    public void readsClosuresFromMetadataMap() {
        Map<String, ShapeClosure> closures = ShapeClosure.fromModel(model.getMetadata());

        assertThat(closures.keySet(), contains("com.example#primary", "com.example#secondary"));
    }

    @Test
    public void fromModelReturnsEmptyMapWhenNoMetadata() {
        assertThat(ShapeClosure.fromModel(Model.builder().build()), equalTo(Collections.emptyMap()));
    }

    @Test
    public void omitsEmptyMembersFromNode() {
        ShapeClosure closure = ShapeClosure.builder().id("com.example#empty").build();

        ObjectNode serialized = closure.toNode().expectObjectNode();
        assertFalse(serialized.getMember("includeNamespaces").isPresent());
        assertFalse(serialized.getMember("includeBySelector").isPresent());
        assertFalse(serialized.getMember("rename").isPresent());
    }

    @Test
    public void requiresId() {
        assertThrows(IllegalStateException.class, () -> ShapeClosure.builder().build());
    }

    @Test
    public void sourceLocationIsExcludedFromEquality() {
        ShapeClosure withLocation = ShapeClosure.builder()
                .id("com.example#a")
                .sourceLocation(new SourceLocation("a.smithy", 1, 1))
                .build();
        ShapeClosure withoutLocation = ShapeClosure.builder().id("com.example#a").build();

        assertThat(withLocation, equalTo(withoutLocation));
        assertEquals(withLocation.hashCode(), withoutLocation.hashCode());
    }
}
