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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
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

                // Detects Smithy IDL 1.0 shapes that were marked as nullable using the box trait. When a structure
                // member is loaded from a 1.0 model, the member is "upgraded" from v1 to v2 in the semantic model.
                // Any member that was implicitly nullable in v1 gets a synthetic box trait on the member. Box traits
                // are not allowed in 2.0 models, so this check is specifically here to ensure that the intended
                // nullability semantics of 1.0 models are honored and not to interfere with 2.0 nullability semantics.
                if (member.hasTrait(BoxTrait.class)) {
                    return true;
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
     * <p>This means that the default trait is ignored, the required trait
     * is ignored, and only the box trait and sparse traits are used.
     *
     * <p>Use {@link #isMemberNullable(MemberShape)} to check using Smithy
     * IDL 2.0 semantics that take required, default, and other traits
     * into account. That method also accurately returns the nullability of
     * 1.0 members as long as the model it's checking was sent through a
     * ModelAssembler.
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
                if (member.hasTrait(BoxTrait.class)) {
                    // The box trait is still around in memory and the model hasn't been reserialized,
                    // then the shape is for sure nullable in v1.
                    return true;
                } else if (member.hasTrait(AddedDefaultTrait.class)) {
                    // If the default trait was added to a member post-hoc, then v1 model semantics should ignore it.
                    return true;
                } else if (isNullable(member.getTarget())) {
                    // Does the target shape still have a box trait in memory and the shape hasn't been reserialized?
                    // Then it's for sure nullable in v1.
                    return true;
                } else {
                    return !isShapeSetToDefaultZeroValue(member, target);
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

    private boolean isShapeSetToDefaultZeroValue(MemberShape member, Shape target) {
        DefaultTrait defaultTrait = member.getTrait(DefaultTrait.class).orElse(null);
        if (defaultTrait == null) {
            return false;
        } else if (target instanceof NumberShape && !(target.isBigDecimalShape() || target.isBigIntegerShape())) {
            // Number shapes are considered non-nullable in IDL 1.0 only if set to 0.
            return defaultTrait.toNode()
                    .asNumberNode()
                    .map(NumberNode::getValue)
                    .filter(value -> value.longValue() == 0)
                    .isPresent();
        } else if (target.isBooleanShape()) {
            // Boolean shapes are considered non-nullable in IDL 1.0 only if set to false.
            return defaultTrait.toNode()
                    .asBooleanNode()
                    .map(BooleanNode::getValue)
                    .filter(value -> !value)
                    .isPresent();
        }
        return false;
    }
}
