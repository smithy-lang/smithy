/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.ExternalDocumentationTrait;

public class MemberShapeTest {
    @Test
    public void returnsTargetShapeId() {
        MemberShape shape = MemberShape.builder().id("ns.foo#bar$baz").target("ns.foo#baz").build();

        assertEquals("ns.foo#baz", shape.getTarget().toString());
    }

    @Test
    public void mustContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            MemberShape.builder().id("ns.foo#bar").target("ns.foo#bar").build();
        });
    }

    @Test
    public void returnsType() {
        MemberShape shape = MemberShape.builder()
                .id(ShapeId.from("ns.foo#bar$baz"))
                .target("ns.foo#baz")
                .build();
        assertEquals(ShapeType.MEMBER, shape.getType());
    }

    @Test
    public void checksForEquality() {
        MemberShape a = MemberShape.builder()
                .id(ShapeId.from("ns.foo#bar$baz"))
                .target("ns.foo#baz")
                .build();
        // Same exact shape.
        assertEquals(a, a);
        // Same semantic shape.
        assertEquals(a, a.toBuilder().build());
        // Different shape member ID.
        assertNotEquals(a, a.toBuilder().target("ns.abc#baz").build());
        // Different shape ID.
        assertNotEquals(a, a.toBuilder().id("ns.other#shape$member").build());
    }

    @Test
    public void equality() {
        MemberShape a = MemberShape.builder()
                .id(ShapeId.from("ns.foo#bar$baz"))
                .target("ns.foo#baz")
                .build();
        assertEquals(a.hashCode(), a.toBuilder().build().hashCode());
    }

    @Test
    public void requiresShapeTargetId() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            MemberShape.builder().id("ns.foo#bar$baz").build();
        });
    }

    @Test
    public void getsMemberTraits() {
        Shape target = StringShape.builder()
                .id("foo.baz#Bar")
                .addTrait(new DocumentationTrait("hi"))
                .addTrait(ExternalDocumentationTrait.builder().addUrl("Ref", "http://example.com").build())
                .build();
        MemberShape member = MemberShape.builder()
                .id("foo.baz#Bar$member")
                .target(target)
                .addTrait(new DocumentationTrait("override"))
                .build();
        Model model = Model.builder().addShapes(member, target).build();

        assertThat(
                member.getMemberTrait(model, DocumentationTrait.class).get().getValue(),
                equalTo("override"));
        assertThat(
                member.getMemberTrait(model, ExternalDocumentationTrait.class).get().getUrls().get("Ref"),
                equalTo("http://example.com"));
        assertThat(
                member.findMemberTrait(model, "documentation"),
                equalTo(member.findTrait("documentation")));
        assertThat(
                member.findMemberTrait(model, "externalDocumentation"),
                equalTo(target.findTrait("externalDocumentation")));
    }
}
