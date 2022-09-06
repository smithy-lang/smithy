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
import java.util.Set;
import java.util.function.Function;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AddedDefaultTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidatedResult;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.Validator;
import software.amazon.smithy.utils.SetUtils;

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

    /** Shapes that were boxed in 1.0, but @box was removed in 2.0. */
    private static final Set<ShapeId> HAD_BOX_TRAIT_IN_V1 = SetUtils.of(
            ShapeId.from("smithy.api#Boolean"),
            ShapeId.from("smithy.api#Byte"),
            ShapeId.from("smithy.api#Short"),
            ShapeId.from("smithy.api#Integer"),
            ShapeId.from("smithy.api#Long"),
            ShapeId.from("smithy.api#Float"),
            ShapeId.from("smithy.api#Double"));

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
        // TODO: can these transforms be more efficiently moved into the loader?
        for (StructureShape struct : model.getStructureShapes()) {
            if (!Prelude.isPreludeShape(struct)) {
                if (fileToVersion.apply(struct) == Version.VERSION_1_0) {
                    // Upgrade structure members in v1 models to add the default trait if needed. Upgrading unknown
                    // versions can cause things like projections to build a model, feed it back into the assembler,
                    // and then lose the original context of which version the model was built with, causing it to be
                    // errantly upgraded.
                    for (MemberShape member : struct.getAllMembers().values()) {
                        model.getShape(member.getTarget()).ifPresent(target -> {
                            upgradeV1Member(member, target);
                        });
                    }
                } else if (fileToVersion.apply(struct) == Version.VERSION_2_0) {
                    // Path v2 based members with the box trait when necessary in order for v1 based tooling to
                    // correctly interpret a v2 model.
                    for (MemberShape member : struct.getAllMembers().values()) {
                        model.getShape(member.getTarget()).ifPresent(target -> {
                            patchV2MemberForV1Support(member, target);
                        });
                    }
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
        } else if (needsSyntheticBoxTraitOnMember(member, target)) {
            shapeUpgrades.add(member.toBuilder().addTrait(new BoxTrait()).build());
        }
    }

    private boolean needsSyntheticBoxTraitOnMember(MemberShape member, Shape target) {
        // Only do this for prelude shapes that had the box trait removed.
        if (HAD_BOX_TRAIT_IN_V1.contains(target.getId())) {
            // Don't add a box trait if it already has one.
            if (!member.hasTrait(BoxTrait.class)) {
                // Don't add a box trait if it has a default trait.
                if (!member.hasTrait(DefaultTrait.class)) {
                    return true;
                }
            }
        }

        return false;
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

    private boolean shouldV1MemberHaveDefaultTrait(MemberShape member, Shape target) {
        // Only when the targeted shape had a default value by default in v1 or if
        // the member has the http payload trait and targets a streaming blob, which
        // implies a default in 2.0
        return (HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType()) || isDefaultPayload(member, target))
            // Don't re-add the @default trait
            && !member.hasTrait(DefaultTrait.ID)
            // Don't add the default trait if the member has clientOptional.
            && !member.hasTrait(ClientOptionalTrait.class)
            // Don't add a @default trait if the member or target are considered boxed in v1.
            && memberAndTargetAreNotAlreadyBoxedInV1(member, target);
    }

    private boolean memberAndTargetAreNotAlreadyBoxedInV1(MemberShape member, Shape target) {
        return memberAndTargetAreNotAlreadyExplicitlyBoxed(member, target)
               // Some prelude shapes are considered boxed in v1 models.
               && !HAD_BOX_TRAIT_IN_V1.contains(target.getId())
               // Check for the boxing tag here to avoid possible ordering issues when loading models and
               // patching in synthetic box traits on target shapes.
               && !target.hasTag("box-v1");
    }

    private boolean isDefaultPayload(MemberShape member, Shape target) {
        // httpPayload requires that the member is required or default.
        // No need to add the default trait if the member is required.
        if (member.isRequired()) {
            return false;
        } else {
            return target.hasTrait(StreamingTrait.ID) && target.isBlobShape();
        }
    }

    private void patchV2MemberForV1Support(MemberShape member, Shape target) {
        // Only apply box to members where the trait can be applied.
        if (canBoxTargetThisKindOfShape(target) && memberAndTargetAreNotAlreadyExplicitlyBoxed(member, target)) {
            if (isMemberInherentlyBoxedInV1(member) || memberDoesNotHaveDefaultZeroValueTrait(member, target)) {
                // The member should be considered boxed by v1 implementations:
                // * The member can be boxed.
                // * The member isn't already boxed based on the member or target shape. v1 implementations will
                //   already look to the target for the box trait.
                // * It has no default zero value. It might have a default, but if the default isn't the zero
                //   value, then it's ignored by v1 implementations.
                shapeUpgrades.add(member.toBuilder().addTrait(new BoxTrait()).build());
            }
        }
    }

    // Only apply box to members where the trait can be applied. Note that intEnum is treated
    // like a normal integer in v1 implementations, so it is allowed to be synthetically boxed.
    private boolean canBoxTargetThisKindOfShape(Shape target) {
        return HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType()) || target.isIntEnumShape();
    }

    private boolean memberAndTargetAreNotAlreadyExplicitlyBoxed(MemberShape member, Shape target) {
        return !member.hasTrait(BoxTrait.ID) && !target.hasTrait(BoxTrait.ID);
    }

    // If the shape has no box trait but does have the addedDefault trait, then v1 Smithy implementations
    // should consider the member boxed because the default trait was added after initially shipping the
    // member. The same is true for the clientOptional trait -- it's a requirement for v2 client generators to
    // make the shape optional, and since v1 generators don't use the updated nullability semantics, this
    // trait should make all v1 implementations treat the member as nullable.
    private boolean isMemberInherentlyBoxedInV1(MemberShape member) {
        return member.hasTrait(AddedDefaultTrait.class) || member.hasTrait(ClientOptionalTrait.class);
    }

    // If the member has a default trait set to the zero value, then consider the member
    // "un-boxed". At this point, if the member does not fit into this category, then member is
    // not considered boxed.
    private boolean memberDoesNotHaveDefaultZeroValueTrait(MemberShape member, Shape target) {
        return !NullableIndex.isShapeSetToDefaultZeroValueInV1(member, target);
    }
}
