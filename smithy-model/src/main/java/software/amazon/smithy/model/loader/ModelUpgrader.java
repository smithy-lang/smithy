/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;

/**
 * Upgrades Smithy models from IDL v1 to IDL v2, specifically taking into
 * account the removal of the box trait, the change in default value
 * semantics of numbers and booleans, and the @default trait.
 */
@SuppressWarnings("deprecation")
final class ModelUpgrader {

    /** Shape types in Smithy 1.0 that had a default value. */
    private static final EnumSet<ShapeType> HAD_DEFAULT_VALUE_IN_1_0 = EnumSet.of(
            ShapeType.BYTE,
            ShapeType.SHORT,
            ShapeType.INTEGER,
            ShapeType.LONG,
            ShapeType.FLOAT,
            ShapeType.DOUBLE,
            ShapeType.BOOLEAN);

    private final Model model;
    private final List<ValidationEvent> events;
    private final Function<Shape, Version> fileToVersion;
    private final List<Shape> shapeUpgrades = new ArrayList<>();

    ModelUpgrader(Model model, List<ValidationEvent> events, Function<Shape, Version> fileToVersion) {
        this.model = model;
        this.events = events;
        this.fileToVersion = fileToVersion;
    }

    ValidatedResult<Model> transform() {
        // Upgrade structure members in v1 models to add the default trait if needed. Upgrading unknown versions
        // can cause things like projections to build a model, feed it back into the assembler, and then lose the
        // original context of which version the model was built with, causing it to be errantly upgraded.
        for (StructureShape struct : model.getStructureShapes()) {
            if (!Prelude.isPreludeShape(struct) && fileToVersion.apply(struct) == Version.VERSION_1_0) {
                for (MemberShape member : struct.getAllMembers().values()) {
                    model.getShape(member.getTarget()).ifPresent(target -> {
                        upgradeV1Member(member, target);
                    });
                }
            }
        }

        return new ValidatedResult<>(ModelTransformer.create().replaceShapes(model, shapeUpgrades), events);
    }

    private void upgradeV1Member(MemberShape member, Shape target) {
        if (shouldV1MemberHaveDefaultTrait(member, target)) {
            // Add the @default trait to structure members when needed.
            events.add(ValidationEvent.builder()
                               .id(Validator.MODEL_DEPRECATION)
                               .severity(Severity.WARNING)
                               .shape(member)
                               .message("Add the @default trait to this member to make it forward compatible with "
                                        + "Smithy IDL 2.0")
                               .build());
            MemberShape.Builder builder = member.toBuilder();
            if (target.isBooleanShape()) {
                builder.addTrait(new DefaultTrait(new BooleanNode(false, builder.getSourceLocation())));
            } else if (target.isBlobShape()) {
                builder.addTrait(new DefaultTrait(new StringNode("", builder.getSourceLocation())));
            } else if (isZeroValidDefault(member)) {
                builder.addTrait(new DefaultTrait(new NumberNode(0, builder.getSourceLocation())));
            }
            shapeUpgrades.add(builder.build());
        } else if (isMemberImplicitlyBoxed(member, target)) {
            // Add a synthetic box trait to the shape.
            MemberShape.Builder builder = member.toBuilder();
            builder.addTrait(new BoxTrait());
            shapeUpgrades.add(builder.build());
        }
    }

    // If it's for sure a v1 shape and was implicitly boxed, then add a synthetic box trait to the member
    // so that tooling that works with both v1 and v2 shapes can know if a shape was considered nullable in 1.0.
    // This is particularly important if the member is required. A required member in 2.0 semantics is considered
    // non-nullable, but considered nullable in 1.0 if the member targets a primitive shape. However, once a v1
    // model is loaded into memory, tooling no longer can differentiate between required in 1.0 or required in
    // 2.0. With these synthetic box traits, tooling can look for the box trait on a member to detect v1
    // nullability semantics.
    private boolean isMemberImplicitlyBoxed(MemberShape member, Shape target) {
        return !member.hasTrait(DefaultTrait.class) // don't add box if it has a default trait.
               && !member.hasTrait(BoxTrait.class) // don't add box again
               && target.hasTrait(BoxTrait.class);
    }

    private boolean isZeroValidDefault(MemberShape member) {
        Optional<RangeTrait> rangeTraitOptional = member.getMemberTrait(model, RangeTrait.class);
        // No range means 0 is fine.
        if (!rangeTraitOptional.isPresent()) {
            return true;
        }
        RangeTrait rangeTrait = rangeTraitOptional.get();

        // Min is greater than 0.
        if (rangeTrait.getMin().isPresent() && rangeTrait.getMin().get().compareTo(BigDecimal.ZERO) > 0) {
            events.add(ValidationEvent.builder()
                    .id(Validator.MODEL_DEPRECATION)
                    .severity(Severity.WARNING)
                    .shape(member)
                    .message("Cannot add the @default trait to this member due to a minimum range constraint.")
                    .build());
            return false;
        }

        // Max is less than 0.
        if (rangeTrait.getMax().isPresent() && rangeTrait.getMax().get().compareTo(BigDecimal.ZERO) < 0) {
            events.add(ValidationEvent.builder()
                    .id(Validator.MODEL_DEPRECATION)
                    .severity(Severity.WARNING)
                    .shape(member)
                    .message("Cannot add the @default trait to this member due to a maximum range constraint.")
                    .build());
            return false;
        }

        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean shouldV1MemberHaveDefaultTrait(MemberShape member, Shape target) {
        // Only when the targeted shape had a default value by default in v1 or if
        // the member has the http payload trait and targets a streaming blob, which
        // implies a default in 2.0
        return (HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType()) || isDefaultPayload(target))
            // Don't re-add the @default trait
            && !member.hasTrait(DefaultTrait.ID)
            // Don't add a @default trait if it will conflict with the @required trait.
            && !member.hasTrait(RequiredTrait.ID)
            // Don't add a @default trait if the member was explicitly boxed in v1.
            && !member.hasTrait(BoxTrait.ID)
            // Don't add a @default trait if the targeted shape was explicitly boxed in v1.
            && !target.hasTrait(BoxTrait.ID);
    }

    private boolean isDefaultPayload(Shape target) {
        return target.hasTrait(StreamingTrait.ID) && target.isBlobShape();
    }
}
