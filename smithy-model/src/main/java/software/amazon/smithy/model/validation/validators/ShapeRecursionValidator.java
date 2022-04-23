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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Ensures that list, set, and map shapes are not directly recursive,
 * meaning that if they do have a recursive reference to themselves,
 * one or more references that form the recursive path travels through
 * a structure or union shape. And ensures that structure members are
 * not infinitely mutually recursive using the required trait.
 *
 * <p>This check removes an entire class of problems from things like
 * code generators where a list of itself or a list of maps of itself
 * is impossible to define.
 */
public final class ShapeRecursionValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        PathFinder finder = PathFinder.create(model);
        List<ValidationEvent> events = new ArrayList<>();
        validateListMapSetShapes(finder, model, events);
        validateStructurePaths(finder, model, events);
        return events;
    }

    private void validateListMapSetShapes(PathFinder finder, Model model, List<ValidationEvent> events) {
        finder.relationshipFilter(rel -> !(rel.getShape().isStructureShape() || rel.getShape().isUnionShape()));

        for (ListShape shape : model.getListShapes()) {
            validateListMapSetShapes(shape, finder, events);
        }

        for (SetShape shape : model.getSetShapes()) {
            validateListMapSetShapes(shape, finder, events);
        }

        for (MapShape shape : model.getMapShapes()) {
            validateListMapSetShapes(shape, finder, events);
        }

        finder.relationshipFilter(FunctionalUtils.alwaysTrue());
    }

    private void validateListMapSetShapes(Shape shape, PathFinder finder, List<ValidationEvent> events) {
        for (PathFinder.Path path : finder.search(shape, Collections.singletonList(shape))) {
            events.add(error(shape, String.format(
                    "Found invalid shape recursion: %s. A recursive list, set, or map shape is only "
                    + "valid if an intermediate reference is through a union or structure.", formatPath(path))));
        }
    }

    private void validateStructurePaths(PathFinder finder, Model model, List<ValidationEvent> events) {
        finder.relationshipFilter(rel -> {
            if (rel.getShape().isStructureShape()) {
                return rel.getNeighborShape().get().hasTrait(RequiredTrait.class);
            } else {
                return rel.getShape().isMemberShape();
            }
        });

        for (StructureShape shape : model.getStructureShapes()) {
            for (PathFinder.Path path : finder.search(shape, Collections.singletonList(shape))) {
                events.add(error(shape, String.format(
                        "Found invalid shape recursion: %s. A structure cannot be mutually recursive through all "
                        + "required members.", formatPath(path))));
            }
        }
    }

    private String formatPath(PathFinder.Path path) {
        StringJoiner joiner = new StringJoiner(" > ");
        List<Shape> shapes = path.getShapes();
        for (int i = 0; i < shapes.size(); i++) {
            // Skip the first shape (the subject) to shorten the error message.
            if (i > 0) {
                joiner.add(shapes.get(i).getId().toString());
            }
        }
        return joiner.toString();
    }
}
