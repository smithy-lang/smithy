/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;

/**
 *  Reorders members of structure and union shapes using a {@link Comparator}.
 */
final class SortMembers {

    private final Comparator<MemberShape> comparator;

    SortMembers(Comparator<MemberShape> comparator) {
        this.comparator = comparator;
    }

    Model transform(ModelTransformer transformer, Model model) {
        Set<Shape> toReplace = new HashSet<>();

        model.shapes(StructureShape.class).forEach(structure -> {
            if (!structure.getAllMembers().isEmpty()) {
                Set<MemberShape> members = new TreeSet<>(comparator);
                members.addAll(structure.getAllMembers().values());
                toReplace.add(structure.toBuilder().members(members).build());
            }
        });

        model.shapes(UnionShape.class).forEach(structure -> {
            if (!structure.getAllMembers().isEmpty()) {
                Set<MemberShape> members = new TreeSet<>(comparator);
                members.addAll(structure.getAllMembers().values());
                toReplace.add(structure.toBuilder().members(members).build());
            }
        });

        return transformer.replaceShapes(model, toReplace);
    }
}
