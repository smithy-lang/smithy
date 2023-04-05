/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;

final class NamedMemberUtils {

    private NamedMemberUtils() {
    }

    static Map<String, MemberShape> computeMixinMembers(
            Map<ShapeId, Shape> mixins,
            BuilderRef<Map<String, MemberShape>> members,
            ShapeId shapeId,
            SourceLocation sourceLocation
    ) {
        if (mixins.isEmpty()) {
            return members.copy();
        }

        // Compute mixin members of this shape that inherit traits from mixin members.
        Map<String, MemberShape> computedMembers = new LinkedHashMap<>();
        for (Shape shape : mixins.values()) {
            for (MemberShape member : shape.members()) {
                String name = member.getMemberName();
                if (members.get().containsKey(name)) {
                    MemberShape localMember = members.get().get(name);
                    // Rebuild the member with the proper inherited mixin if needed.
                    // This catches errant cases where a member is added to a structure/union
                    // but omits the mixin members of parent shapes. Arguably, that's way too
                    // nuanced and error-prone to _not_ try to smooth over.
                    if (localMember.getMixins().isEmpty() || !mixins.containsKey(member.getId())) {
                        localMember = localMember.toBuilder().clearMixins().addMixin(member).build();
                    }
                    computedMembers.put(name, localMember);
                } else {
                    MemberShape previouslyDefined = computedMembers.get(name);
                    if (previouslyDefined != null) {
                        validateMixinMemberConflict(member, previouslyDefined);
                        computedMembers.put(name,
                                previouslyDefined.toBuilder()
                                .source(member.getSourceLocation())
                                .addMixin(member)
                                .addTraits(member.getAllTraits().values())
                                .build());
                    } else {
                        computedMembers.put(name, MemberShape.builder()
                                .id(shapeId.withMember(name))
                                .target(member.getTarget())
                                .source(member.getSourceLocation())
                                .addMixin(member)
                                .build());
                    }
                }
            }
        }

        // Add members local to the structure after inherited members.
        for (MemberShape member : members.get().values()) {
            if (!computedMembers.containsKey(member.getMemberName())) {
                computedMembers.put(member.getMemberName(), member);
            }
        }
        return Collections.unmodifiableMap(computedMembers);
    }

    static Set<MemberShape> flattenMixins(
            Map<String, MemberShape> members,
            Map<ShapeId, Shape> mixins,
            ShapeId shapeId,
            SourceLocation sourceLocation
    ) {
        // Ensure that the members are ordered, mixin members first, followed by local members.
        Map<String, MemberShape> orderedMembers = new LinkedHashMap<>();
        Map<String, MemberShape> pendingMembers = new LinkedHashMap<>();

        // Copy members from mixins onto the shape.
        for (Shape mixin : mixins.values()) {
            for (MemberShape member : mixin.members()) {
                String memberName = member.getMemberName();
                MemberShape existing = members.remove(memberName);
                if (existing != null) {
                    // Save it for later processing, we cannot set the location and introduced traits
                    // here, if multiple mixins override this member then they will overwrite each other.
                    pendingMembers.put(memberName, existing);
                }
                MemberShape previouslyDefined = orderedMembers.get(memberName);
                Collection<? extends Trait> previousTraits = Collections.emptyList();
                if (previouslyDefined != null) {
                    previousTraits = previouslyDefined.getAllTraits().values();
                    validateMixinMemberConflict(member, previouslyDefined);
                }
                orderedMembers.put(memberName, MemberShape.builder()
                        .id(shapeId.withMember(memberName))
                        .target(member.getTarget())
                        .addTraits(previousTraits)
                        .addTraits(member.getAllTraits().values())
                        .source(member.getSourceLocation())
                        .build());
            }
        }

        for (Map.Entry<String, MemberShape> entry : pendingMembers.entrySet()) {
            String memberName = entry.getKey();
            MemberShape existing = entry.getValue();
            MemberShape needUpdate = orderedMembers.get(memberName);
            orderedMembers.put(memberName, needUpdate.toBuilder()
                    .source(existing.getSourceLocation())
                    .addTraits(existing.getIntroducedTraits().values())
                    .build());
        }

        // Add any local members _after_ mixin members. LinkedHashMap will keep insertion
        // order, so no need to check for non-mixin members first.
        orderedMembers.putAll(members);
        return new LinkedHashSet<>(orderedMembers.values());
    }

    static void validateMixinMemberConflict(MemberShape member, MemberShape previouslyDefined) {
        if (!previouslyDefined.getTarget().equals(member.getTarget())) {
            throw new SourceException("Member conflicts with an inherited mixin member: `"
                    + previouslyDefined.getId() + "`", member);
        }
    }

    static void cleanMixins(Shape newMixin, Map<String, MemberShape> members) {
        // Clean up members that were previously mixed in by the given shape but
        // are no longer present on the given shape.
        members.values().removeIf(member -> {
            if (!isMemberMixedInFromShape(newMixin.getId(), member)) {
                return false;
            }
            for (MemberShape mixinMember : newMixin.members()) {
                if (mixinMember.getMemberName().equals(member.getMemberName())) {
                    return false;
                }
            }
            return true;
        });
    }

    static void removeMixin(ToShapeId mixin, Map<String, MemberShape> members) {
        ShapeId id = mixin.toShapeId();
        // Remove members that have a mixin where the ID equals the given ID or
        // the mixin ID without a member equals the given ID.
        members.values().removeIf(member -> isMemberMixedInFromShape(id, member));
    }

    private static boolean isMemberMixedInFromShape(ShapeId check, MemberShape member) {
        if (member.getMixins().contains(check)) {
            return true;
        }
        for (ShapeId memberMixin : member.getMixins()) {
            if (memberMixin.withoutMember().equals(check)) {
                return true;
            }
        }
        return false;
    }
}
