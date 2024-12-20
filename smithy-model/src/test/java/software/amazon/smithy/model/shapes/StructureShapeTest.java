/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.DocumentationTrait;
import software.amazon.smithy.model.traits.InternalTrait;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.SensitiveTrait;

public class StructureShapeTest {
    @Test
    public void returnsAppropriateType() {
        StructureShape shape = StructureShape.builder().id("ns.foo#bar").build();

        assertEquals(shape.getType(), ShapeType.STRUCTURE);
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(SourceException.class, () -> {
            StructureShape.builder().id("ns.foo#bar$baz").build();
        });
    }

    @Test
    public void addMemberWithTarget() {
        StructureShape shape = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder().id(shape.getId().withMember("foo")).target("ns.foo#bam").build());
    }

    @Test
    public void addMemberWithConsumer() {
        StructureShape shape = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"), builder -> builder.addTrait(new SensitiveTrait()))
                .build();

        assertEquals(shape.getMember("foo").get(),
                MemberShape.builder()
                        .id(shape.getId().withMember("foo"))
                        .target("ns.foo#bam")
                        .addTrait(new SensitiveTrait())
                        .build());
    }

    @Test
    public void returnsMembers() {
        StructureShape shape = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .build();

        assertThat(shape.members(), hasSize(2));
        // Members are ordered.
        assertThat(shape.members(), contains(shape.getMember("foo").get(), shape.getMember("baz").get()));
        assertThat(shape.getAllMembers().keySet(), contains("foo", "baz"));
    }

    @Test
    public void memberOrderMattersForEqualComparison() {
        StructureShape a = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .build();

        StructureShape b = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .build();

        assertThat(a, not(equalTo(b)));
    }

    @Test
    public void builderUpdatesMemberIds() {
        StructureShape original = StructureShape.builder()
                .id("ns.foo#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .build();

        StructureShape actual = original.toBuilder().id(ShapeId.from("ns.bar#bar")).build();

        StructureShape expected = StructureShape.builder()
                .id("ns.bar#bar")
                .addMember("foo", ShapeId.from("ns.foo#bam"))
                .addMember("baz", ShapeId.from("ns.foo#bam"))
                .build();

        assertThat(actual, equalTo(expected));
    }

    @Test
    public void supportsMixinTraits() {
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#Mixin1")
                .addTrait(new SensitiveTrait())
                .build();
        StructureShape mixin2 = StructureShape.builder()
                .id("smithy.example#Mixin2")
                .addTrait(DeprecatedTrait.builder().build())
                .build();
        StructureShape mixin3 = StructureShape.builder()
                .id("smithy.example#Mixin3")
                .addMixin(mixin2)
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                .addMixin(mixin1)
                .addMixin(mixin3)
                .addTrait(new DocumentationTrait("hi"))
                .build();

        assertTrue(concrete.hasTrait(SensitiveTrait.class));
        assertTrue(concrete.hasTrait(DeprecatedTrait.class));
        assertTrue(concrete.hasTrait(DocumentationTrait.class));
    }

    @Test
    public void reordersMixinMembersAutomatically() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#Mixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId())
                .build();
        StructureShape mixin2 = StructureShape.builder()
                .id("smithy.example#Mixin2")
                .addTrait(MixinTrait.builder().build())
                .addMember("b", string.getId())
                .build();
        StructureShape mixin3 = StructureShape.builder()
                .id("smithy.example#Mixin3")
                .addTrait(MixinTrait.builder().build())
                .addMember("c", string.getId())
                .addMixin(mixin2)
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                // Note that d is added before mixins, but the builder tracks this
                // and handles ordering appropriately when building the shape.
                .addMember("d", string.getId())
                .addMixin(mixin1)
                .addMixin(mixin3)
                .build();

        assertThat(concrete.getMemberNames(), contains("a", "b", "c", "d"));
    }

    @Test
    public void mixinMembersCanBeModifiedJustLikeNormalMembers() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#Mixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId())
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                .addMixin(mixin1)
                .build();

        // Just like you'd do with a normal member, you first get the member,
        // then convert it to a builder to update it, then build it. Then the
        // member is added to a new container shape and rebuilt. The workflow
        // is exactly the same as a normal structure with no mixin members.
        MemberShape updatedA = concrete.getMember("a")
                .get()
                .toBuilder()
                .addTrait(new SensitiveTrait())
                .build();
        StructureShape updated = concrete.toBuilder().addMember(updatedA).build();

        assertThat(updated.getMemberNames(), contains("a"));
        assertTrue(updated.getMember("a").get().hasTrait(SensitiveTrait.class));
        assertThat(updated.getMember("a").get().getMixins(), contains(mixin1.getMember("a").get().getId()));
    }

    @Test
    public void structuresAccountForMissingMixinsOnLocalMembers() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#TestMixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId(), builder -> builder.addTrait(new SensitiveTrait()))
                .addMember("b", string.getId())
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                // The missing mixin is automatically added to the computed members because the mixin
                // is added after the member is added. If the member is added after the mixin, then
                // this safeguard doesn't work.
                .addMember(MemberShape.builder().id("smithy.example#Concrete$a").target(string).build())
                // Note that b becomes a local member because it introduces a new trait.
                .addMember("b", string.getId(), builder -> builder.addTrait(new SensitiveTrait()))
                .addMixin(mixin1)
                .build();

        assertTrue(concrete.getMember("a").get().hasTrait(SensitiveTrait.class));
        assertThat(concrete.getMember("a").get().getMixins(), contains(mixin1.getMember("a").get().getId()));

        assertTrue(concrete.getMember("b").get().hasTrait(SensitiveTrait.class));
        assertThat(concrete.getMember("b").get().getMixins(), contains(mixin1.getMember("b").get().getId()));
    }

    @Test
    public void flattensMixins() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#TestMixin1")
                .addTrait(MixinTrait.builder().addLocalTrait(InternalTrait.ID).build())
                .addTrait(DeprecatedTrait.builder().build())
                .addTrait(new InternalTrait()) // local and not copied.
                .addMember("a", string.getId(), builder -> builder.addTrait(new SensitiveTrait()))
                .addMember("b", string.getId())
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                .addTrait(new SensitiveTrait())
                .addMixin(mixin1)
                .addMember("b", string.getId(), builder -> builder.addTrait(DeprecatedTrait.builder().build()))
                .addMember("c", string.getId())
                .build();

        StructureShape flattened = concrete.toBuilder().flattenMixins().build();
        StructureShape expected = StructureShape.builder()
                .id("smithy.example#Concrete")
                .addTrait(new SensitiveTrait())
                .addTrait(DeprecatedTrait.builder().build())
                .addMember("a", string.getId(), builder -> builder.addTrait(new SensitiveTrait()))
                .addMember("b", string.getId(), builder -> builder.addTrait(DeprecatedTrait.builder().build()))
                .addMember("c", string.getId())
                .build();

        assertThat(flattened, equalTo(expected));
        assertThat(flattened.getMemberNames(), contains("a", "b", "c"));
    }

    @Test
    public void flatteningStructureWithNoMixinsDoesNothing() {
        StructureShape shape = StructureShape.builder().id("smithy.example#A").flattenMixins().build();

        assertThat(shape, equalTo(StructureShape.builder().id("smithy.example#A").build()));
    }

    @Test
    public void redefiningMembersPreservesOrder() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#Mixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId())
                .addMember("b", string.getId())
                .addMember("c", string.getId())
                .build();
        StructureShape mixin2 = StructureShape.builder()
                .id("smithy.example#Mixin2")
                .addTrait(MixinTrait.builder().build())
                .addMember("c", string.getId())
                .addMember("b", string.getId())
                .addMember("a", string.getId())
                .build();
        StructureShape mixin3 = StructureShape.builder()
                .id("smithy.example#Mixin3")
                .addTrait(MixinTrait.builder().build())
                .addMember("b", string.getId())
                .addMember("c", string.getId())
                .addMember("a", string.getId())
                .addMixin(mixin2)
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                // Note that d is added before mixins, but the builder tracks this
                // and handles ordering appropriately when building the shape.
                .addMember("d", string.getId())
                .addMixin(mixin1)
                .addMixin(mixin3)
                .build();

        assertThat(concrete.getMemberNames(), contains("a", "b", "c", "d"));
    }

    @Test
    public void fixesMissingMemberMixins() {
        StringShape string = StringShape.builder().id("smithy.example#String").build();
        StructureShape mixin1 = StructureShape.builder()
                .id("smithy.example#Mixin1")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId())
                .build();
        StructureShape mixin2 = StructureShape.builder()
                .id("smithy.example#Mixin2")
                .addTrait(MixinTrait.builder().build())
                .addMember("a", string.getId())
                .build();
        StructureShape concrete = StructureShape.builder()
                .id("smithy.example#Concrete")
                .addMember("a", string.getId())
                .addMixin(mixin1)
                .addMixin(mixin2)
                .build();

        assertThat(concrete.getMember("a").get().getMixins(),
                contains(
                        ShapeId.from("smithy.example#Mixin1$a"),
                        ShapeId.from("smithy.example#Mixin2$a")));
    }
}
