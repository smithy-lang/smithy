/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
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
        assertThat(shape, is(shape.expectMemberShape()));
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
                .addTrait(new ExternalDocumentationTrait("http://example.com"))
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
                member.getMemberTrait(model, ExternalDocumentationTrait.class).get().getValue(),
                equalTo("http://example.com"));
        assertThat(
                member.findMemberTrait(model, "documentation"),
                equalTo(member.findTrait("documentation")));
        assertThat(
                member.findMemberTrait(model, "externalDocumentation"),
                equalTo(target.findTrait("externalDocumentation")));
    }
}
