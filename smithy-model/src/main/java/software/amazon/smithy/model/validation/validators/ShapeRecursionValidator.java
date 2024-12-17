/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.PathFinder;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
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
        validateUnions(model, events);
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
            events.add(error(shape,
                    String.format(
                            "Found invalid shape recursion: %s. A recursive list, set, or map shape is only "
                                    + "valid if an intermediate reference is through a union or structure.",
                            formatPath(path))));
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
                events.add(error(shape,
                        String.format(
                                "Found invalid shape recursion: %s. A structure cannot be mutually recursive through all "
                                        + "required members.",
                                formatPath(path))));
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

    private void validateUnions(Model model, List<ValidationEvent> events) {
        UnionTerminatesVisitor visitor = new UnionTerminatesVisitor(model);
        for (UnionShape union : model.getUnionShapes()) {
            // Don't evaluate empty unions since that's a different error.
            if (!union.members().isEmpty() && !union.accept(visitor)) {
                events.add(error(union, "It is impossible to create instances of this recursive union"));
            }
            visitor.reset();
        }
    }

    private static final class UnionTerminatesVisitor extends ShapeVisitor.Default<Boolean> {

        private final Set<MemberShape> visited = new HashSet<>();
        private final Model model;

        UnionTerminatesVisitor(Model model) {
            this.model = model;
        }

        void reset() {
            this.visited.clear();
        }

        @Override
        protected Boolean getDefault(Shape shape) {
            return true;
        }

        @Override
        public Boolean structureShape(StructureShape shape) {
            if (shape.members().isEmpty()) {
                return true;
            }

            // If the structure has any non-required members, then it terminates.
            for (MemberShape member : shape.members()) {
                if (!member.isRequired()) {
                    return true;
                }
            }

            // Now check if any of the required members terminate.
            for (MemberShape member : shape.members()) {
                if (member.isRequired()) {
                    if (memberShape(member)) {
                        return true;
                    }
                }
            }

            return false;
        }

        @Override
        public Boolean unionShape(UnionShape shape) {
            for (MemberShape member : shape.members()) {
                if (memberShape(member)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public Boolean memberShape(MemberShape shape) {
            return visited.add(shape)
                    ? model.expectShape(shape.getTarget()).accept(this)
                    : false;
        }
    }
}
