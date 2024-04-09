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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AddedDefaultTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.transform.ModelTransformer;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures interoperability between IDL 1 and IDL 2 models by adding default traits and synthetic box traits where
 * needed to normalize across versions.
 *
 * <p>For 1 to 2 interop, this class adds default traits. If a root level v1 shape is not boxed and supports the box
 * trait, a default trait is added set to the zero value of the type. If a v1 member is marked with the box trait,
 * then a default trait is added to the member set to null. If a member is targets a streaming blob and is not
 * required, the default trait is added set to an empty string.
 *
 * <p>For 2 to 1 interop, if a v2 member has a default set to null a synthetic box trait is added. If a root level
 * does not have the default trait and the type supports the box trait, a synthetic box trait is added.
 */
@SuppressWarnings("deprecation")
final class ModelInteropTransformer {

    /** Shape types in Smithy 1.0 that had a default value. */
    private static final EnumSet<ShapeType> HAD_DEFAULT_VALUE_IN_1_0 = EnumSet.of(
            ShapeType.BYTE,
            ShapeType.SHORT,
            ShapeType.INTEGER,
            ShapeType.LONG,
            ShapeType.FLOAT,
            ShapeType.DOUBLE,
            ShapeType.BOOLEAN,
            ShapeType.INT_ENUM); // intEnum is actually an integer in v1, but the ShapeType is different.

    private final Model model;
    private final List<ValidationEvent> events;
    private final Function<Shape, Version> fileToVersion;
    private final List<Shape> shapeUpgrades = new ArrayList<>();

    ModelInteropTransformer(Model model, List<ValidationEvent> mutableEvents, Function<Shape, Version> fileToVersion) {
        this.model = model;
        this.events = mutableEvents;
        this.fileToVersion = fileToVersion;
    }

    Model transform() {
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
                    // Patch v2 based members with the box trait when necessary in order for v1 based tooling to
                    // correctly interpret a v2 model.
                    for (MemberShape member : struct.getAllMembers().values()) {
                        model.getShape(member.getTarget()).ifPresent(target -> {
                            patchV2MemberForV1Support(member, target);
                        });
                    }
                }
            }
        }

        return ModelTransformer.create().replaceShapes(model, shapeUpgrades);
    }

    private void upgradeV1Member(MemberShape member, Shape target) {
        if (shouldV1MemberHaveDefaultTrait(member, target)) {
            // Add the @default trait to structure members when the target shapes in V1 models have an
            // implicit default trait due to a zero value (e.g., PrimitiveInteger).
            MemberShape.Builder builder = member.toBuilder();
            Node defaultValue = getDefaultValueOfType(member, target.getType());
            builder.addTrait(new DefaultTrait(defaultValue));
            shapeUpgrades.add(builder.build());
        } else if (member.hasTrait(BoxTrait.class)) {
            // Add a default trait to the member set to null to indicate it was boxed in v1.
            MemberShape.Builder builder = member.toBuilder();
            builder.addTrait(new DefaultTrait(new NullNode(member.getSourceLocation())));
            shapeUpgrades.add(builder.build());
        }
    }

    private boolean shouldV1MemberHaveDefaultTrait(MemberShape member, Shape target) {
        // Only when the targeted shape had a default value by default in v1 or if
        // the member targets a streaming blob, which implies a default in 2.0
        return (HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType()) || streamingBlobNeedsDefault(member, target))
            // Don't re-add the @default trait
            && !member.hasTrait(DefaultTrait.ID)
            // Don't add a @default trait if the member or target are considered boxed in v1.
            && memberAndTargetAreNotAlreadyExplicitlyBoxed(member, target);
    }

    private boolean streamingBlobNeedsDefault(MemberShape member, Shape target) {
        // Streaming blobs require that the member is required or default.
        // No need to add the default trait if the member is required.
        if (member.isRequired()) {
            return false;
        } else {
            return target.hasTrait(StreamingTrait.ID) && target.isBlobShape();
        }
    }

    private void patchV2MemberForV1Support(MemberShape member, Shape target) {
        if (!canBoxTargetThisKindOfShape(target)) {
            return;
        }

        if (!memberDoesNotHaveDefaultZeroValueTrait(member, target)) {
            return;
        }

        if (member.hasNullDefault() || v2ShapeNeedsBoxTrait(member, target)) {
            // Add a synthetic box trait to the member because either:
            // * it's default value is set to null.
            // * it has the addedDefault trait.
            // * it not already explicitly boxed
            shapeUpgrades.add(member.toBuilder().addTrait(new BoxTrait()).build());
        }
    }

    private boolean v2ShapeNeedsBoxTrait(MemberShape member, Shape target) {
        return isMemberInherentlyBoxedInV1(member)
               && memberAndTargetAreNotAlreadyExplicitlyBoxed(member, target);
    }

    // Only apply box to members where the trait can be applied.
    private boolean canBoxTargetThisKindOfShape(Shape target) {
        return HAD_DEFAULT_VALUE_IN_1_0.contains(target.getType());
    }

    private boolean memberAndTargetAreNotAlreadyExplicitlyBoxed(MemberShape member, Shape target) {
        return !member.hasTrait(BoxTrait.ID) && !target.hasTrait(BoxTrait.ID);
    }

    // The addedDefault trait implies that a member did not previously have a default, and a default value
    // was added later. In this case, naive box implementation can assume the member is boxed.
    private boolean isMemberInherentlyBoxedInV1(MemberShape member) {
        return member.hasTrait(AddedDefaultTrait.class);
    }

    // If the member has a default trait set to the zero value, then consider the member
    // "un-boxed". At this point, if the member does not fit into this category, then member is
    // not considered boxed.
    private boolean memberDoesNotHaveDefaultZeroValueTrait(MemberShape member, Shape target) {
        return !NullableIndex.isShapeSetToDefaultZeroValueInV1(member, target);
    }

    static void patchShapeBeforeBuilding(
            LoadOperation.DefineShape defineShape,
            AbstractShapeBuilder<?, ?> builder,
            List<ValidationEvent> events
    ) {
        handleBoxing(defineShape, builder);
    }

    private static void handleBoxing(LoadOperation.DefineShape defineShape, AbstractShapeBuilder<?, ?> builder) {
        // Only need to modify shapes that had a zero value in v1.
        if (!HAD_DEFAULT_VALUE_IN_1_0.contains(builder.getShapeType())) {
            return;
        }

        // Special casing to add synthetic box traits onto root level shapes for v1 compatibility.
        if (defineShape.version == Version.VERSION_1_0) {
            // Add default traits to shapes that support them.
            if (!isBuilderBoxed(builder)) {
                Node defaultZeroValue = getDefaultValueOfType(builder, builder.getShapeType());
                DefaultTrait syntheticDefault = new DefaultTrait(defaultZeroValue);
                builder.addTrait(syntheticDefault);
            }
        } else if (defineShape.version == Version.VERSION_2_0) {
            // Add the box trait to root level shapes not marked with the default trait, or that are marked with
            // the default trait, and it isn't set to the zero value of a v1 type.
            Trait defaultTrait = builder.getAllTraits().get(DefaultTrait.ID);
            Node defaultValue = defaultTrait == null ? null : defaultTrait.toNode();
            boolean isDefaultZeroValue = NullableIndex
                    .isDefaultZeroValueOfTypeInV1(defaultValue, defineShape.getShapeType());
            if (!isDefaultZeroValue) {
                builder.addTrait(new BoxTrait());
            }
        }
    }

    private static boolean isBuilderBoxed(AbstractShapeBuilder<?, ?> builder) {
        return builder.getAllTraits().containsKey(BoxTrait.ID);
    }

    private static Node getDefaultValueOfType(FromSourceLocation sourceLocation, ShapeType type) {
        // Includes all possible types that support default values, though this class currently uses only
        // boolean, numeric types, and blobs.
        switch (type) {
            case BOOLEAN:
                return new BooleanNode(false, sourceLocation.getSourceLocation());
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM:
            case LONG:
            case FLOAT:
            case DOUBLE:
            case BIG_DECIMAL:
            case BIG_INTEGER:
                return new NumberNode(0, sourceLocation.getSourceLocation());
            case BLOB:
            case STRING:
                return new StringNode("", sourceLocation.getSourceLocation());
            case LIST:
            case SET:
                return new ArrayNode(Collections.emptyList(), sourceLocation.getSourceLocation());
            case MAP:
                return new ObjectNode(Collections.emptyMap(), sourceLocation.getSourceLocation());
            case DOCUMENT:
                return new NullNode(sourceLocation.getSourceLocation());
            default:
                throw new UnsupportedOperationException("Unexpected shape type: " + type);
        }
    }
}
