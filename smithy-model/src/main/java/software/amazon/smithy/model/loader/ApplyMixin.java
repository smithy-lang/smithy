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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Applies mixins to shapes after the mixins have been resolved.
 */
final class ApplyMixin implements ShapeModifier {
    private final ShapeId mixin;
    private List<ValidationEvent> events;

    ApplyMixin(ShapeId mixin) {
        this.mixin = mixin;
    }

    @Override
    public void modifyMember(
            AbstractShapeBuilder<?, ?> shapeBuilder,
            MemberShape.Builder memberBuilder,
            Function<ShapeId, Map<ShapeId, Trait>> unclaimedTraits,
            Function<ShapeId, Shape> shapeMap
    ) {
        // The target could have been set by resource based properties.
        if (memberBuilder.getTarget() != null) {
            return;
        }
        // Members inherited from mixins can have their targets elided, so here we set them
        // to the target defined in the mixin.
        Shape mixinShape = shapeMap.apply(mixin);
        if (mixinShape == null) {
            throw new SourceException("Cannot apply mixin to " + memberBuilder.getId() + ": " + mixin + " not found",
                                      memberBuilder);
        }

        String name = memberBuilder.getId().getMember().get();
        mixinShape.getMember(name).ifPresent(mixinMember -> memberBuilder.target(mixinMember.getTarget()));
    }

    @Override
    public void modifyShape(
            AbstractShapeBuilder<?, ?> builder,
            Map<String, MemberShape.Builder> memberBuilders,
            Function<ShapeId, Map<ShapeId, Trait>> unclaimedTraits,
            Function<ShapeId, Shape> shapeMap
    ) {
        Shape mixinShape = shapeMap.apply(mixin);
        if (mixinShape == null) {
            throw new SourceException("Cannot apply mixin to " + builder.getId() + ": " + mixin + " not found",
                                      builder);
        }

        for (MemberShape member : mixinShape.members()) {
            ShapeId targetId = builder.getId().withMember(member.getMemberName());
            // Claim traits from the trait map that were applied to synthesized shapes.
            Map<ShapeId, Trait> introducedTraits = unclaimedTraits.apply(targetId);
            String memberName = member.getMemberName();
            MemberShape introducedMember = null;
            Optional<MemberShape> previouslyAdded = builder.getMember(memberName);
            if (previouslyAdded.isPresent()) {
                // The member was previously introduced, check if it inherits from another mixin to
                // avoid overwriting it.
                MemberShape previous = previouslyAdded.get();
                if (!previous.getMixins().isEmpty()) {
                    MemberShape.Builder previouslyAddedBuilder = previous.toBuilder();
                    introducedMember = previouslyAddedBuilder
                            .addMixin(member)
                            .build();
                    if (!previous.getTarget().equals(member.getTarget())) {
                        mixinMemberConflict(previouslyAddedBuilder, member);
                    }
                }
            }
            if (introducedMember == null && memberBuilders.containsKey(member.getMemberName())) {
                MemberShape.Builder original = memberBuilders.get(member.getMemberName());
                introducedMember = original.addMixin(member).build();
                if (!introducedMember.getTarget().equals(member.getTarget())) {
                    mixinMemberConflict(original, member);
                }
            } else if (!introducedTraits.isEmpty()) {
                // Build local member copies before adding mixins if traits
                // were introduced to inherited mixin members.
                introducedMember = MemberShape.builder()
                        .id(targetId)
                        .target(member.getTarget())
                        .source(member.getSourceLocation())
                        .addTraits(introducedTraits.values())
                        .addMixin(member)
                        .build();
            }

            if (introducedMember != null) {
                builder.addMember(introducedMember);
            }
        }

        builder.addMixin(mixinShape);
    }

    private void mixinMemberConflict(MemberShape.Builder conflict, MemberShape other) {
        if (events == null) {
            events = new ArrayList<>();
        }
        events.add(ValidationEvent.builder()
                .severity(Severity.ERROR)
                .id(Validator.MODEL_ERROR)
                .shapeId(conflict.getId())
                .sourceLocation(conflict.getSourceLocation())
                .message("Member conflicts with an inherited mixin member: `" + other.getId() + "`")
                .build());
    }

    @Override
    public List<ValidationEvent> getEvents() {
        return events == null ? Collections.emptyList() : events;
    }
}
