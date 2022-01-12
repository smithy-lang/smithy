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
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.traits.SparseTrait;

/**
 * An index that checks if a shape can be set to null.
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
     * Checks if the given shape is optional.
     *
     * @param shape Shape or shape ID to check.
     * @return Returns true if the shape is optional.
     */
    public final boolean isNullable(ToShapeId shape) {
        Model m = Objects.requireNonNull(model.get());
        Shape s = m.expectShape(shape.toShapeId());
        MemberShape member = s.asMemberShape().orElse(null);
        // Non-members should always be considered optional.
        return member == null || isMemberOptional(member);
    }

    /**
     * Checks if a member is optional.
     *
     * @param member Member to check.
     * @return Returns true if the member is optional.
     */
    public boolean isMemberOptional(MemberShape member) {
        Model m = Objects.requireNonNull(model.get());
        Shape container = m.expectShape(member.getContainer());

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
