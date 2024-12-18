/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.Objects;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
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
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An index that checks if a member is nullable.
 */
public class NullableIndex implements KnowledgeIndex {

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
         * Evaluates only default traits on members that are set to their zero value based on Smithy
         * IDL 1.0 semantics (that is, only looks at shapes that had a zero value in Smithy v1, including byte,
         * short, integer, long, float, double, and boolean. If a member is marked with addedDefault or with
         * clientOptional or is in an input structure, then the member is always considered nullable.
         */
        @SmithyUnstableApi
        CLIENT_ZERO_VALUE_V1 {
            @Override
            boolean isStructureMemberOptional(StructureShape container, MemberShape member, Shape target) {
                return container.hasTrait(InputTrait.class)
                        || CLIENT_ZERO_VALUE_V1_NO_INPUT.isStructureMemberOptional(container, member, target);
            }
        },

        /**
         * Evaluates only default traits on members that are set to their zero value based on Smithy
         * IDL 1.0 semantics (that is, only looks at shapes that had a zero value in Smithy v1, including byte,
         * short, integer, long, float, double, and boolean. If a member is marked with addedDefault or with
         * clientOptional, then the member is always considered nullable.
         */
        @SmithyUnstableApi
        CLIENT_ZERO_VALUE_V1_NO_INPUT {
            @Override
            boolean isStructureMemberOptional(StructureShape container, MemberShape member, Shape target) {
                if (member.hasTrait(AddedDefaultTrait.class) || member.hasTrait(ClientOptionalTrait.class)) {
                    return true;
                }

                return !isShapeSetToDefaultZeroValueInV1(member, target);
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
                // Evaluated in this order.
                // 1. Does the member have the required trait? Stop further checks, it's non-optional.
                // 2. Does the member have a default trait set to null? Stop further checks, it's optional.
                // 3. Does the member have a default trait not set to null? Stop further checks, it's non-optional.
                return !member.hasTrait(RequiredTrait.class) && !member.hasNonNullDefault();
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
        }

        switch (shape.getType()) {
            case MEMBER:
                return isMemberNullableInV1(m, shape.asMemberShape().get());
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INTEGER:
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
                return isMemberNullable(member, CheckMode.CLIENT_ZERO_VALUE_V1_NO_INPUT);
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
        DefaultTrait memberDefault = member.getTrait(DefaultTrait.class).orElse(null);
        Node defaultValue = memberDefault == null ? null : memberDefault.toNode();

        // No default or null default values on members are considered nullable.
        if (defaultValue == null || defaultValue.isNullNode()) {
            return false;
        }

        ShapeType targetType = target.getType();
        return isDefaultZeroValueOfTypeInV1(defaultValue, targetType);
    }

    /**
     * Detects if the given node value equals the default value of the given shape type
     * based on Smithy 1.0 semantics.
     *
     * @param defaultValue Value to check.
     * @param targetType Shape type to check against.
     * @return Returns true if the value is the v1 zero value of the type.
     */
    @SmithyUnstableApi
    public static boolean isDefaultZeroValueOfTypeInV1(Node defaultValue, ShapeType targetType) {
        if (defaultValue == null) {
            return false;
        }

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
