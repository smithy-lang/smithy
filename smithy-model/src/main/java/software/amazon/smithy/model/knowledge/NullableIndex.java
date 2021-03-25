/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashSet;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ToShapeId;
import software.amazon.smithy.model.traits.BoxTrait;
import software.amazon.smithy.model.traits.SparseTrait;
import software.amazon.smithy.utils.SetUtils;

/**
 * An index that checks if a shape can be set to null.
 */
public class NullableIndex implements KnowledgeIndex {

    private static final Set<ShapeType> INHERENTLY_BOXED = SetUtils.of(
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

    private final Set<ShapeId> nullableShapes = new HashSet<>();

    public NullableIndex(Model model) {
        for (Shape shape : model.toSet()) {
            if (shape.asMemberShape().isPresent()) {
                if (isMemberNullable(model, shape.asMemberShape().get())) {
                    nullableShapes.add(shape.getId());
                }
            } else if (isShapeBoxed(shape)) {
                nullableShapes.add(shape.getId());
            }
        }
    }

    public static NullableIndex of(Model model) {
        return model.getKnowledge(NullableIndex.class, NullableIndex::new);
    }

    private static boolean isMemberNullable(Model model, MemberShape member) {
        Shape container = model.getShape(member.getContainer()).orElse(null);

        // Ignore broken models in this index. Other validators handle these checks.
        if (container == null) {
            return false;
        }

        switch (container.getType()) {
            case STRUCTURE:
                // Only structure shapes look at the box trait.
                return member.hasTrait(BoxTrait.class)
                       || model.getShape(member.getTarget()).filter(NullableIndex::isShapeBoxed).isPresent();
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

    private static boolean isShapeBoxed(Shape shape) {
        return INHERENTLY_BOXED.contains(shape.getType()) || shape.hasTrait(BoxTrait.class);
    }

    /**
     * Checks if the given shape can be set to null.
     *
     * <p>When given a list member or map value member, this method will
     * return true if and only if the container of the member is marked with
     * the {@link SparseTrait}. When given a member of a structure, this
     * method will return true if and only if the member is marked with the
     * {@link BoxTrait}, the targeted shape is marked with the {@code box}
     * trait, or if the targeted shape is inherently boxed. When given a set
     * member, union member, or map key member, this method will always
     * return false. When given any other shape, this method will return
     * true if the shape is inherently boxed, meaning the shape is either
     * marked with the {@code box} trait, or the shape is a string, blob,
     * timestamp, bigDecimal, bigInteger, list, set, map, structure, union or document.
     *
     * @param shape Shape or shape ID to check.
     * @return Returns true if the shape can be set to null.
     */
    public final boolean isNullable(ToShapeId shape) {
        return nullableShapes.contains(shape.toShapeId());
    }
}
