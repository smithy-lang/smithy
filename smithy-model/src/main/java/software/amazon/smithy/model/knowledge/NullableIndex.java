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

package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.AddedDefaultTrait;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An index that checks if a member is nullable.
 */
public class NullableIndex implements KnowledgeIndex {

    /** Shapes that were boxed in 1.0, but @box was removed in 2.0. */
    private static final Set<ShapeId> PREVIOUSLY_BOXED = SetUtils.of(
            ShapeId.from("smithy.api#Boolean"),
            ShapeId.from("smithy.api#Byte"),
            ShapeId.from("smithy.api#Short"),
            ShapeId.from("smithy.api#Integer"),
            ShapeId.from("smithy.api#Long"),
            ShapeId.from("smithy.api#Float"),
            ShapeId.from("smithy.api#Double"));

    private final WeakReference<Model> model;

    public NullableIndex(Model model) {
        this.model = new WeakReference<>(model);
    }

    public static NullableIndex of(Model model) {
        return model.getKnowledge(NullableIndex.class, NullableIndex::new);
    }

    /**
     * Defines the type of model consumer to assume when determining if
     * a member should be considered nullable or always present.
     */
    public enum CheckMode {
        /**
         * A client, or any other kind of non-authoritative model consumer
         * that must honor the {@link InputTrait} and {@link ClientOptionalTrait}.
         */
        CLIENT {
            @Override
            boolean isStructureMemberOptional(StructureShape container, MemberShape member, Shape target) {
                if (member.hasTrait(ClientOptionalTrait.class) || container.hasTrait(InputTrait.class)) {
                    return true;
                }

                return SERVER.isStructureMemberOptional(container, member, target);
            }
        },

        /**
         * Like {@link #CLIENT} mode, but will treat all members that target
         * structures and unions as optional because these members can never
         * transition to optional using a default trait.
         */
        CLIENT_CAREFUL {
            @Override
            boolean isStructureMemberOptional(StructureShape container, MemberShape member, Shape target) {
                if (target instanceof StructureShape || target instanceof UnionShape) {
                    return true;
                }

                return CLIENT.isStructureMemberOptional(container, member, target);
            }
        },

        /**
         * A server, or any other kind of authoritative model consumer
         * that does not honor the {@link InputTrait} and {@link ClientOptionalTrait}.
         *
         * <p>This mode should only be used for model consumers that have
         * perfect knowledge of the model because they are built and deployed
         * in lock-step with model updates. A client does not have perfect
         * knowledge of a model because it has to be generated, deployed,
         * and then migrated to when model updates are released. However, a
         * server is required to be updated in order to implement newly added
         * model components.
         */
        SERVER {
            @Override
            boolean isStructureMemberOptional(StructureShape container, MemberShape member, Shape target) {
                // A member with the default trait is never nullable. Note that even 1.0 models will be
                // assigned a default trait when they are "upgraded".
                if (member.hasTrait(DefaultTrait.class)) {
                    return false;
                }

                // A 2.0 member with the required trait is never nullable.
                return !member.hasTrait(RequiredTrait.class);
            }
        };

        abstract boolean isStructureMemberOptional(StructureShape container, MemberShape member, Shape target);
    }

    /**
     * Checks if a member is nullable using {@link CheckMode#CLIENT}.
     *
     * @param member Member to check.
     * @return Returns true if the member is optional in
     *         non-authoritative consumers of the model like clients.
     * @see #isMemberNullable(MemberShape, CheckMode)
     */
    public boolean isMemberNullable(MemberShape member) {
        return isMemberNullable(member, CheckMode.CLIENT);
    }

    /**
     * Checks if a member is nullable using v2 nullability rules.
     *
     * <p>A {@code checkMode} parameter is required to declare what kind of
     * model consumer is checking if the member is optional. The authoritative
     * consumers like servers do not need to honor the {@link InputTrait} or
     * {@link ClientOptionalTrait}, while non-authoritative consumers like clients
     * must honor these traits.
     *
     * @param member Member to check.
     * @param checkMode The mode used when checking if the member is considered nullable.
     * @return Returns true if the member is optional.
     */
    public boolean isMemberNullable(MemberShape member, CheckMode checkMode) {
        Model m = Objects.requireNonNull(model.get());
        Shape container = m.expectShape(member.getContainer());
        Shape target = m.expectShape(member.getTarget());

        switch (container.getType()) {
            case STRUCTURE:
                return checkMode.isStructureMemberOptional(container.asStructureShape().get(), member, target);
            case UNION:
            case SET:
                // Union and set members are never null.
                return false;
            case MAP:
                // Map keys are never null.
                if (member.getMemberName().equals("key")) {
                    return false;
                }
                // fall-through.
            case LIST:
                // Map values and list members are only null if they have the @sparse trait.
                return container.hasTrait(SparseTrait.class);
            default:
                return false;
        }
    }

    /**
     * Checks if the given shape is optional using Smithy IDL 1.0 semantics.
     *
     * <p>This method does not return the same values that are returned by
     * {@link #isMemberNullable(MemberShape)}. This method uses 1.0 model
     * semantics and attempts to detect when a model has been passed though
     * model assembler upgrades to provide the most accurate v1 nullability
     * result.
     *
     * <p>Use {@link #isMemberNullable(MemberShape)} to check using Smithy
     * IDL 2.0 semantics that take required, default, and other traits
     * into account with no special 1.0 handling.
     *
     * @param shapeId Shape or shape ID to check.
     * @return Returns true if the shape is nullable.
     */
    @Deprecated
    public final boolean isNullable(ToShapeId shapeId) {
        Model m = Objects.requireNonNull(model.get());
        Shape shape = m.getShape(shapeId.toShapeId()).orElse(null);

        if (shape == null) {
            return false;
        } else if (PREVIOUSLY_BOXED.contains(shape.getId())) {
            // Special case root-level checks of prelude shapes that were considered boxed in v1.
            // This special casing is not used when checking member targets because the NullableIndex
            // relies on the ModelAssembler to place box traits on members when it needs to determine that
            // a member is nullable.
            return true;
        } else if (shape.isMemberShape()) {
            return isMemberNullableInV1(m, shape.asMemberShape().get());
        } else {
            return isRootLevelShapeNullable(shape);
        }
    }

    private boolean isRootLevelShapeNullable(Shape shape) {
        switch (shape.getType()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM: // treat INT_ENUM like integer shapes.
            case LONG:
            case FLOAT:
            case DOUBLE:
                return shape.hasTrait(BoxTrait.class);
            default:
                return true;
        }
    }

    private boolean isMemberNullableInV1(Model model, MemberShape member) {
        Shape container = model.getShape(member.getContainer()).orElse(null);
        Shape target = model.getShape(member.getTarget()).orElse(null);

        // Ignore broken models in this index. Other validators handle these checks.
        if (container == null || target == null) {
            return false;
        }

        switch (container.getType()) {
            case STRUCTURE:
                if (member.hasTrait(BoxTrait.class)) {
                    // The box trait makes a member nullable in v1.
                    return true;
                } else if (member.hasTrait(AddedDefaultTrait.class)) {
                    // If the default trait was added to a member post-hoc, then v1 model semantics should ignore it.
                    return true;
                } else if (isShapeSetToDefaultZeroValueInV1(member, target)) {
                    // If set to the default zero value on the member, then it is non-nullable.
                    return false;
                } else {
                    // Check if the target has a box trait in memory. Prelude shapes will not have a box trait, but
                    // model assembler will ensure that a box is added to members in this situation so that earlier
                    // checks in this method will deem the member as nullable.
                    return isRootLevelShapeNullable(target);
                }
            case MAP:
                // Map keys can never be null.
                if (member.getMemberName().equals("key")) {
                    return false;
                } // fall-through
            case LIST:
                // Sparse lists and maps are considered nullable.
                return container.hasTrait(SparseTrait.class);
            default:
                return false;
        }
    }

    /**
     * Detects if the given member is configured to use the zero value for the target shape
     * using Smithy 1.0 semantics (that is, it targets a number shape other than bigInteger
     * or bigDecimal and set to 0; or it targets a boolean shape and is set to false).
     *
     * @param member Member to check.
     * @param target Shape target to check.
     * @return Returns true if the member has a default trait set to a v1 zero value.
     */
    @SmithyUnstableApi
    public static boolean isShapeSetToDefaultZeroValueInV1(MemberShape member, Shape target) {
        if (!member.hasTrait(DefaultTrait.ID)) {
            return false;
        }

        ShapeType targetType = target.getType();
        Node defaultValue = member.getAllTraits().get(DefaultTrait.ID).toNode();

        switch (targetType) {
            case BOOLEAN:
                return defaultValue
                        .asBooleanNode()
                        .map(BooleanNode::getValue)
                        .filter(value -> !value)
                        .isPresent();
            case BYTE:
            case SHORT:
            case INTEGER:
            case INT_ENUM: // v1 models treat intEnum like a normal enum.
            case LONG:
            case FLOAT:
            case DOUBLE:
                return defaultValue.asNumberNode().filter(NumberNode::isZero).isPresent();
            default:
                return false;
        }
    }
}
