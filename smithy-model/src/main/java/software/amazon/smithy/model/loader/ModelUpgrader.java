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
        for (MemberShape member : model.getMemberShapes()) {
            if (Prelude.isPreludeShape(member)) {
                continue;
            }

            Version version = fileToVersion.apply(member);
            if (version == Version.VERSION_2_0) {
                validateV2Member(member);
            } else {
                // Also attempt to upgrade unknown versions since they could be 1.0 and
                // trying to upgrade 2.0 shapes has no effect.
                // For v1 shape checks, we need to know the containing shape type to apply the appropriate transform.
                model.getShape(member.getContainer())
                        .ifPresent(container -> upgradeV1Member(version, container.getType(), member));
            }
        }

        return new ValidatedResult<>(ModelTransformer.create().replaceShapes(model, shapeUpgrades), events);
    }

    private void upgradeV1Member(Version version, ShapeType containerType, MemberShape member) {
        // Don't fail here on broken models, and since it's broken, don't try to upgrade it.
        Shape target = model.getShape(member.getTarget()).orElse(null);
        if (target == null) {
            return;
        }

        // This builder will become non-null if/when the member needs to be updated.
        MemberShape.Builder builder = null;

        // Add the @default trait to structure members when needed.
        if (shouldV1MemberHaveDefaultTrait(containerType, member, target)) {
            events.add(ValidationEvent.builder()
                               .id(Validator.MODEL_DEPRECATION)
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
            } else if (isZeroValidDefault(member)) {
                builder.addTrait(new DefaultTrait(new NumberNode(0, builder.getSourceLocation())));
            }
        } else if (isMemberImplicitlyBoxed(version, containerType, member, target)) {
            // Add a synthetic box trait to the shape.
            builder = createOrReuseBuilder(member, builder).addTrait(new BoxTrait());
        }

        if (builder != null) {
            shapeUpgrades.add(builder.build());
        }
    }

    // If it's for sure a v1 shape and was implicitly boxed, then add a synthetic box trait so tooling
    // can know that the shape was previously considered nullable. Note that this method does not
    // check if the targeted shape is required. It's up to tooling to determine how to handle a 1.0
    // member that is both required and boxed.
    private boolean isMemberImplicitlyBoxed(
            Version version,
            ShapeType containerType,
            MemberShape member,
            Shape target
    ) {
        return version == Version.VERSION_1_0
               && containerType == ShapeType.STRUCTURE
               && !member.hasTrait(DefaultTrait.class) // don't add box if it has a default trait.
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
    private boolean shouldV1MemberHaveDefaultTrait(ShapeType containerType, MemberShape member, Shape target) {
        return containerType == ShapeType.STRUCTURE
            // Only when the targeted shape had a default value by default in v1 or if
            // the member has the http payload trait and targets a streaming blob, which
            // implies a default in 2.0
            && (HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType()) || isDefaultPayload(target))
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

    @SuppressWarnings("deprecation")
    private void validateV2Member(MemberShape member) {
        if (member.hasTrait(BoxTrait.class)) {
            events.add(ValidationEvent.builder()
                               .id(Validator.MODEL_DEPRECATION)
                               .severity(Severity.ERROR)
                               .shape(member)
                               .sourceLocation(member.expectTrait(BoxTrait.class))
                               .message("@box is not supported in Smithy IDL 2.0")
                               .build());
        }
    }
}
