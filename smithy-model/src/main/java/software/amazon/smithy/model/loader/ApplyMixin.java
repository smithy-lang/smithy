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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
final class ApplyMixin implements PendingShapeModifier {
    private final ShapeId mixin;
    private final List<ValidationEvent> events;

    ApplyMixin(ShapeId mixin, List<ValidationEvent> events) {
        this.mixin = mixin;
        this.events = events;
    }

    @Override
    public Set<ShapeId> getDependencies() {
        return Collections.singleton(mixin);
    }

    @Override
    public void modifyMember(
            AbstractShapeBuilder<?, ?> shapeBuilder,
            MemberShape.Builder memberBuilder,
            TraitContainer resolvedTraits,
            Map<ShapeId, Shape> shapeMap
    ) {
        // Fast-fail the common case.
        if (memberBuilder.getTarget() != null) {
            return;
        }

        // Members inherited from mixins can have their targets elided, so here we set them
        // to the target defined in the mixin.
        Shape mixinShape = shapeMap.get(mixin);
        String name = memberBuilder.getId().getMember().get();
        if (mixinShape.getMember(name).isPresent()) {
            memberBuilder.target(mixinShape.getMember(name).get().getTarget());
        }
    }

    @Override
    public void modifyShape(
            AbstractShapeBuilder<?, ?> builder,
            Map<String, MemberShape.Builder> memberBuilders,
            TraitContainer resolvedTraits,
            Map<ShapeId, Shape> shapeMap
    ) {
        Shape mixinShape = shapeMap.get(mixin);
        for (MemberShape member : mixinShape.members()) {
            ShapeId targetId = builder.getId().withMember(member.getMemberName());
            Map<ShapeId, Trait> introducedTraits = resolvedTraits.getTraitsForShape(targetId);

            MemberShape introducedMember = null;
            if (memberBuilders.containsKey(member.getMemberName())) {
                introducedMember = memberBuilders.get(member.getMemberName())
                        .addMixin(member)
                        .build();

                if (!introducedMember.getTarget().equals(member.getTarget())) {
                    // Members cannot be redefined if their targets conflict.
                    MemberShape.Builder conflict = memberBuilders.get(member.getMemberName());
                    events.add(ValidationEvent.builder()
                            .severity(Severity.ERROR)
                            .id(Validator.MODEL_ERROR)
                            .shapeId(conflict.getId())
                            .sourceLocation(conflict.getSourceLocation())
                            .message("Member conflicts with an inherited mixin member: " + member.getId())
                            .build());
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
}
