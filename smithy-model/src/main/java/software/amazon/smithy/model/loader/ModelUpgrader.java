/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.loader;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.MapUtils;

/**
 * Upgrades Smithy models from IDL v1 to IDL v2, specifically taking into
 * account the removal of the Primitive* shapes from the prelude, the
 * removal of the @box trait, the change in default value semantics of
 * numbers and booleans, and the @default trait.
 */
final class ModelUpgrader {

    private static final String UPGRADE_MODEL = "UpgradeModel";

    /** Shape types in Smithy 1.0 that had a default value. */
    private static final EnumSet<ShapeType> HAD_DEFAULT_VALUE_IN_1_0 = EnumSet.of(
            ShapeType.BYTE,
            ShapeType.SHORT,
            ShapeType.INTEGER,
            ShapeType.LONG,
            ShapeType.FLOAT,
            ShapeType.DOUBLE,
            ShapeType.BOOLEAN);

    /** Provides a mapping of prelude shapes that were removed to the shape to use instead. */
    private static final Map<ShapeId, ShapeId> REMOVED_PRIMITIVE_SHAPES = MapUtils.of(
            ShapeId.from("smithy.api#PrimitiveBoolean"), ShapeId.from("smithy.api#Boolean"),
            ShapeId.from("smithy.api#PrimitiveByte"), ShapeId.from("smithy.api#Byte"),
            ShapeId.from("smithy.api#PrimitiveShort"), ShapeId.from("smithy.api#Short"),
            ShapeId.from("smithy.api#PrimitiveInteger"), ShapeId.from("smithy.api#Integer"),
            ShapeId.from("smithy.api#PrimitiveLong"), ShapeId.from("smithy.api#Long"),
            ShapeId.from("smithy.api#PrimitiveFloat"), ShapeId.from("smithy.api#Float"),
            ShapeId.from("smithy.api#PrimitiveDouble"), ShapeId.from("smithy.api#Double"));

    /** Shapes that were boxed in 1.0, but @box was removed in 2.0. */
    private static final Set<ShapeId> PREVIOUSLY_BOXED = new HashSet<>(REMOVED_PRIMITIVE_SHAPES.values());

    private final Model model;
    private final List<ValidationEvent> events;
    private final Map<String, Version> fileToVersion;
    private final List<Shape> shapeUpgrades = new ArrayList<>();

    ModelUpgrader(Model model, List<ValidationEvent> events, Map<String, Version> fileToVersion) {
        this.model = model;
        this.events = events;
        this.fileToVersion = fileToVersion;
    }

    ValidatedResult<Model> transform() {
        for (MemberShape member : model.getMemberShapes()) {
            // We must assume v2 for manually created shapes.
            Version version = fileToVersion.getOrDefault(member.getSourceLocation().getFilename(),
                                                         Version.VERSION_2_0);
            if (version == Version.VERSION_1_0) {
                // For v1 shape checks, we need to know the containing shape type to apply the appropriate transform.
                model.getShape(member.getContainer()).ifPresent(container -> {
                    upgradeV1Member(container.getType(), member);
                });
            } else if (version == Version.VERSION_2_0) {
                validateV2Member(member);
            }
        }

        return new ValidatedResult<>(ModelTransformer.create().replaceShapes(model, shapeUpgrades), events);
    }

    private void upgradeV1Member(ShapeType containerType, MemberShape member) {
        // Don't fail here on broken models, and since it's broken, don't try to upgrade it.
        Shape target = model.getShape(member.getTarget()).orElse(null);
        if (target == null) {
            return;
        }

        // This builder will become non-null if/when the member needs to be updated.
        MemberShape.Builder builder = null;

        // Rewrite the member to target the non-removed prelude shape if it's known to be removed.
        if (REMOVED_PRIMITIVE_SHAPES.containsKey(target.getId())) {
            emitWhenTargetingRemovedPreludeShape(Severity.WARNING, member);
            builder = createOrReuseBuilder(member, builder);
            builder.target(REMOVED_PRIMITIVE_SHAPES.get(target.getId()));
        }

        // Add the @default trait to structure members when needed.
        if (shouldV1MemberHaveDefaultTrait(containerType, member, target)) {
            events.add(ValidationEvent.builder()
                               .id(UPGRADE_MODEL)
                               .severity(Severity.WARNING)
                               .shape(member)
                               .message("Add the @default trait to this member to make it forward compatible with "
                                        + "Smithy IDL 2.0")
                               .build());
            builder = createOrReuseBuilder(member, builder);

            if (target.isBooleanShape()) {
                builder.addTrait(new DefaultTrait(new BooleanNode(false, builder.getSourceLocation())));
            } else if (target.isBlobShape()) {
                builder.addTrait(new DefaultTrait(new StringNode("", builder.getSourceLocation())));
            } else {
                builder.addTrait(new DefaultTrait(new NumberNode(0, builder.getSourceLocation())));
            }
        }

        if (builder != null) {
            shapeUpgrades.add(builder.build());
        }
    }

    private boolean shouldV1MemberHaveDefaultTrait(ShapeType containerType, MemberShape member, Shape target) {
        return containerType == ShapeType.STRUCTURE
            // Only when the targeted shape had a default value by default in v1 or if
            // the member has the http payload trait and targets a streaming blob, which
            // implies a default in 2.0
            && (HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType()) || isDefaultPayload(target))
            // Not when the targeted shape was one of the prelude types with the @box trait.
            // This needs special handling here because the @box trait was removed from these
            // prelude shapes in v2.
            && !PREVIOUSLY_BOXED.contains(target.getId())
            // Don't re-add the @default trait
            && !member.hasTrait(DefaultTrait.class)
            // Don't add a @default trait if it will conflict with the @required trait.
            && !member.hasTrait(RequiredTrait.class)
            // Don't add a @default trait if the member was explicitly boxed in v1.
            && !member.hasTrait(BoxTrait.class)
            // Don't add a @default trait if the targeted shape was explicitly boxed in v1.
            && !target.hasTrait(BoxTrait.class);
    }

    private boolean isDefaultPayload(Shape target) {
        return target.hasTrait(StreamingTrait.class) && target.isBlobShape();
    }

    private MemberShape.Builder createOrReuseBuilder(MemberShape member, MemberShape.Builder builder) {
        return builder == null ? member.toBuilder() : builder;
    }

    private void validateV2Member(MemberShape member) {
        if (REMOVED_PRIMITIVE_SHAPES.containsKey(member.getTarget())) {
            emitWhenTargetingRemovedPreludeShape(Severity.ERROR, member);
        }

        if (member.hasTrait(BoxTrait.class)) {
            events.add(ValidationEvent.builder()
                               .id(UPGRADE_MODEL)
                               .severity(Severity.ERROR)
                               .shape(member)
                               .sourceLocation(member.expectTrait(BoxTrait.class))
                               .message("@box is not supported in Smithy IDL 2.0")
                               .build());
        }
    }

    private void emitWhenTargetingRemovedPreludeShape(Severity severity, MemberShape member) {
        events.add(ValidationEvent.builder()
                           .id(UPGRADE_MODEL)
                           .severity(severity)
                           .shape(member)
                           .sourceLocation(member)
                           .message("This member targets " + member.getTarget() + " which was removed in Smithy "
                                    + "IDL " + Model.MODEL_VERSION + ". Target "
                                    + REMOVED_PRIMITIVE_SHAPES.get(member.getTarget()) + " instead ")
                           .build());
    }
}
