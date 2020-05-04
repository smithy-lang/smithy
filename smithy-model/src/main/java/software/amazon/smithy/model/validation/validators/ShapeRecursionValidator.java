/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that list, set, and map shapes are not directly recursive,
 * meaning that if they do have a recursive reference to themselves,
 * one or more references that form the recursive path travels through
 * a structure or union shape.
 *
 * <p>This check removes an entire class of problems from things like
 * code generators where a list of itself or a list of maps of itself
 * is impossible to define.
 */
public final class ShapeRecursionValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .map(shape -> validateShape(model, shape))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private ValidationEvent validateShape(Model model, Shape shape) {
        return new RecursiveNeighborVisitor(model, shape).visit(shape);
    }

    private final class RecursiveNeighborVisitor extends ShapeVisitor.Default<ValidationEvent> {

        private final Model model;
        private final Shape root;
        private final Set<ShapeId> visited = new HashSet<>();
        private final Deque<String> context = new ArrayDeque<>();

        RecursiveNeighborVisitor(Model model, Shape root) {
            this.root = root;
            this.model = model;
        }

        ValidationEvent visit(Shape shape) {
            ValidationEvent event = hasShapeBeenVisited(shape);
            return event != null ? event : shape.accept(this);
        }

        private ValidationEvent hasShapeBeenVisited(Shape shape) {
            if (!visited.contains(shape.getId())) {
                return null;
            }

            return error(shape, String.format(
                    "Found invalid shape recursion: %s. A recursive list, set, or map shape is only valid if "
                    + "an intermediate reference is through a union or structure.",
                    String.join(" > ", context)));
        }

        @Override
        protected ValidationEvent getDefault(Shape shape) {
            return null;
        }

        @Override
        public ValidationEvent listShape(ListShape shape) {
            return validateMember(shape, shape.getMember());
        }

        @Override
        public ValidationEvent setShape(SetShape shape) {
            return validateMember(shape, shape.getMember());
        }

        @Override
        public ValidationEvent mapShape(MapShape shape) {
            return validateMember(shape, shape.getValue());
        }

        private ValidationEvent validateMember(Shape container, MemberShape member) {
            ValidationEvent event = null;
            Shape target = model.getShape(member.getTarget()).orElse(null);

            if (target != null) {
                // Add to the visited set and the context deque before visiting,
                // the remove from them after done visiting this shape.
                visited.add(container.getId());
                // Eventually, this would look like: member-id > shape-id[ > member-id > shape-id [ > [...]]
                context.addLast(member.getId().toString());
                context.addLast(member.getTarget().toString());
                event = visit(target);
                context.removeLast();
                context.removeLast();
                visited.remove(container.getId());
            }

            return event;
        }
    }
}
