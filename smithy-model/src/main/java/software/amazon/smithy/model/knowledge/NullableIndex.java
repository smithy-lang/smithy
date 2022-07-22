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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ToShapeId;
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
     * Checks if a member is nullable.
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
    @SuppressWarnings("deprecation")
    public boolean isMemberNullable(MemberShape member, CheckMode checkMode) {
        Model m = Objects.requireNonNull(model.get());
        Shape container = m.expectShape(member.getContainer());

        // Support box trait for 1.0 loaded models that weren't converted to 2.0.
        if (member.hasTrait(BoxTrait.class)) {
            return true;
        }

        // Client mode honors the nullable and input trait.
        if (checkMode == CheckMode.CLIENT
                && (member.hasTrait(ClientOptionalTrait.class) || container.hasTrait(InputTrait.class))) {
            return true;
        }

        switch (container.getType()) {
            case STRUCTURE:
                // Structure members are nullable by default; non-null when marked as @default / @required.
                return !member.hasTrait(DefaultTrait.class) && !member.hasTrait(RequiredTrait.class);
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
}
