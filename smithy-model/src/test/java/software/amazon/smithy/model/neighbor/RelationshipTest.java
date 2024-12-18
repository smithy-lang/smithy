/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.neighbor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;

public class RelationshipTest {
    @Test
    public void hasShortCtor() {
        Shape member = MemberShape.builder().id("ns.foo#List$member").target("ns.foo#String").build();
        Shape target = StringShape.builder().id("ns.foo#String").build();
        Relationship relationship = Relationship.create(member, RelationshipType.MEMBER_TARGET, target);

        assertSame(member, relationship.getShape());
        assertSame(RelationshipType.MEMBER_TARGET, relationship.getRelationshipType());
        assertSame(target.getId(), relationship.getNeighborShapeId());
        assertSame(target, relationship.getNeighborShape().get());
    }

    @Test
    public void getters() {
        Shape member = MemberShape.builder().id("ns.foo#List$member").target("ns.foo#String").build();
        Shape target = StringShape.builder().id("ns.foo#String").build();
        Relationship relationship = Relationship.create(member, RelationshipType.MEMBER_TARGET, target);

        assertSame(member, relationship.getShape());
        assertSame(RelationshipType.MEMBER_TARGET, relationship.getRelationshipType());
        assertSame(target.getId(), relationship.getNeighborShapeId());
        assertSame(target, relationship.getNeighborShape().get());
    }

    @Test
    public void equalsAndHashCode() {
        Shape member = MemberShape.builder().id("ns.foo#List$member").target("ns.foo#String").build();
        Shape target = StringShape.builder().id("ns.foo#String").build();
        Shape otherString = StringShape.builder().id("ns.foo#String2").build();
        Relationship r1 = Relationship.create(member, RelationshipType.MEMBER_TARGET, target);
        Relationship r2 = Relationship.create(member, RelationshipType.MEMBER_TARGET, target);
        Relationship r3 = Relationship.create(target, RelationshipType.MEMBER_TARGET, target);
        Relationship r4 = Relationship.create(member, RelationshipType.READ, target);
        Relationship r5 = Relationship.create(member, RelationshipType.MEMBER_TARGET, otherString);
        Relationship r6 = Relationship.createInvalid(member, RelationshipType.MEMBER_TARGET, target.getId());

        assertNotEquals(r1, "foo");
        assertEquals(r1, r2);
        assertEquals(r2, r1);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3); // different shape
        assertNotEquals(r1.hashCode(), r3.hashCode());
        assertNotEquals(r1, r4); // different type
        assertNotEquals(r1.hashCode(), r4.hashCode());
        assertNotEquals(r1, r5); // different neighbor shape
        assertNotEquals(r1.hashCode(), r5.hashCode());
        assertNotEquals(r1, r6); // neighbor shape missing in one
    }
}
