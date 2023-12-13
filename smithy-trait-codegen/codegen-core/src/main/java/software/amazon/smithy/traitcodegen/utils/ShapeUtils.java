/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.utils;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;

public interface ShapeUtils {
    /**
     * Checks if a given shape defines a trait.
     *
     * @param shape Shape to check.
     * @return Returns true if the shape defines a trait.
     */
    static boolean isTrait(Shape shape) {
        return shape.hasTrait(TraitDefinition.class);
    }


    // TODO: Figure out why this doesn't just work for the existing symbol provider?

    /**
     * Get the name for a member accounting for special naming of list and member members.
     * <p>
     * List and Map members do return a special member name of "values".
     *
     * @param member         Member to get name for.
     * @param model          Model to use to get the member shape's container.
     * @param symbolProvider Symbol provider to use to get member names.
     * @return Returns the name of the member.
     */
    static String toMemberNameOrValues(MemberShape member, Model model, SymbolProvider symbolProvider) {
        Shape containerShape = model.expectShape(member.getContainer());
        if (containerShape.isMapShape() || containerShape.isListShape()) {
            return "values";
        } else {
            return symbolProvider.toMemberName(member);
        }
    }
}
