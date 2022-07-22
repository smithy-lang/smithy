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

package software.amazon.smithy.model.knowledge;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.utils.SetUtils;

/**
 * An index that checks if a member is nullable.
 *
 * <p>Note: this index assumes Smithy 2.0 nullability semantics.
 * There is basic support for detecting 1.0 models by detecting
 * when a removed primitive prelude shape is targeted by a member.
 * Beyond that, 1.0 models SHOULD be loaded through a {@link ModelAssembler}
 * to upgrade them to IDL 2.0.
 */
public class NullableIndex implements KnowledgeIndex {

    private static final Set<ShapeId> V1_REMOVED_PRIMITIVE_SHAPES = SetUtils.of(
            ShapeId.from("smithy.api#PrimitiveBoolean"),
            ShapeId.from("smithy.api#PrimitiveByte"),
            ShapeId.from("smithy.api#PrimitiveShort"),
            ShapeId.from("smithy.api#PrimitiveInteger"),
            ShapeId.from("smithy.api#PrimitiveLong"),
            ShapeId.from("smithy.api#PrimitiveFloat"),
            ShapeId.from("smithy.api#PrimitiveDouble"));

    private static final Set<ShapeType> V1_INHERENTLY_BOXED = SetUtils.of(
            ShapeType.STRING,
            ShapeType.BLOB,
            ShapeType.TIMESTAMP,
            ShapeType.BIG_DECIMAL,
            ShapeType.BIG_INTEGER,
            ShapeType.LIST,
            ShapeType.SET,
            ShapeType.MAP,
            ShapeType.STRUCTURE,
            ShapeType.UNION,
            ShapeType.DOCUMENT);

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
        CLIENT,

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
        SERVER
    }

    /**
     * Checks if the given shape is optional using {@link CheckMode#CLIENT}.
     *
     * @param shape Shape or shape ID to check.
     * @return Returns true if the shape is nullable.
     */
    public final boolean isNullable(ToShapeId shape) {
        return isNullable(shape, CheckMode.CLIENT);
    }

    /**
     * Checks if the given shape is nullable.
     *
     * @param shape Shape or shape ID to check.
     * @param checkMode The mode used when checking if the shape is considered nullable.
     * @return Returns true if the shape is nullable.
     */
    public final boolean isNullable(ToShapeId shape, CheckMode checkMode) {
        Model m = Objects.requireNonNull(model.get());
        Shape s = m.expectShape(shape.toShapeId());
        MemberShape member = s.asMemberShape().orElse(null);
        // Non-members should always be considered nullable.
        return member == null || isMemberNullable(member, checkMode);
    }

    /**
     * Checks if a member is nullable using {@link CheckMode#CLIENT}.
     *
     * @param member Member to check.
     * @return Returns true if the member is optional in
     *  non-authoritative consumers of the model like clients.
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
     * <p>This method will also attempt to detect when a member targets a
     * primitive prelude shape that was removed in Smithy IDL 2.0 to account
     * for models that were created manually without passing through a
     * ModelAssembler. If a member targets a removed primitive prelude shape,
     * the member is considered non-null.
     *
     * @param member Member to check.
     * @param checkMode The mode used when checking if the member is considered nullable.
     * @return Returns true if the member is optional.
     */
    public boolean isMemberNullable(MemberShape member, CheckMode checkMode) {
        Model m = Objects.requireNonNull(model.get());
        Shape container = m.expectShape(member.getContainer());

        switch (container.getType()) {
            case STRUCTURE:
                // Client mode honors the nullable and input trait.
                if (checkMode == CheckMode.CLIENT
                        && (member.hasTrait(ClientOptionalTrait.class) || container.hasTrait(InputTrait.class))) {
                    return true;
                }

                // Structure members that are @required or @default are not nullable.
                if (member.hasTrait(DefaultTrait.class) || member.hasTrait(RequiredTrait.class)) {
                    return false;
                }

                // Detect if the member targets a 1.0 primitive prelude shape and the shape wasn't upgraded.
                // These removed prelude shapes are impossible to appear in a 2.0 model, so it's safe to
                // detect them and honor 1.0 semantics here.
                return !V1_REMOVED_PRIMITIVE_SHAPES.contains(member.getTarget());
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
     * Checks if a member is nullable using v1 nullability rules.
     *
     * <p>This method matches the previous behavior seen in NullableIndex prior
     * to Smithy 1.0. Most models are sent through a ModelAssembler which makes
     * using the normal {@link #isMemberNullable(MemberShape)} the best choice.
     * However, in some cases, a model might get created directly in code
     * using Smithy 1.0 semantics. In those cases, this method can be used to
     * detect if the member is nullable or not.
     *
     * <p>This method ignores the default trait, clientOptional trait,
     * input trait, and required trait.
     *
     * @param member Member to check.
     * @return Returns true if the member is nullable using 1.0 resolution rules.
     */
    public boolean isMemberNullableInV1(MemberShape member) {
        Model m = Objects.requireNonNull(model.get());
        Shape container = m.getShape(member.getContainer()).orElse(null);
        Shape target = m.getShape(member.getTarget()).orElse(null);

        // Ignore broken models in this index. Other validators handle these checks.
        if (container == null || target == null) {
            return false;
        }

        // Defer to 2.0 checks for shapes that aren't structures, since the logic is the same.
        if (container.getType() != ShapeType.STRUCTURE) {
            return isMemberNullable(member);
        }

        // Check if the member or the target has the box trait.
        if (member.getMemberTrait(m, BoxTrait.class).isPresent()) {
            return true;
        }

        return V1_INHERENTLY_BOXED.contains(target.getType());
    }
}
