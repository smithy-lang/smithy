/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.ast.AndExpression;
import software.amazon.smithy.jmespath.ast.ComparatorExpression;
import software.amazon.smithy.jmespath.ast.CurrentExpression;
import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.FieldExpression;
import software.amazon.smithy.jmespath.ast.FilterProjectionExpression;
import software.amazon.smithy.jmespath.ast.FlattenExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectHashExpression;
import software.amazon.smithy.jmespath.ast.MultiSelectListExpression;
import software.amazon.smithy.jmespath.ast.NotExpression;
import software.amazon.smithy.jmespath.ast.ObjectProjectionExpression;
import software.amazon.smithy.jmespath.ast.OrExpression;
import software.amazon.smithy.jmespath.ast.ProjectionExpression;
import software.amazon.smithy.jmespath.ast.SliceExpression;
import software.amazon.smithy.jmespath.ast.Subexpression;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.NotPropertyTrait;
import software.amazon.smithy.model.traits.PropertyTrait;
import software.amazon.smithy.model.traits.ResourceIdentifierTrait;

/**
 * Resolves resource lifecycle identifier and property locations against operation input/output shapes.
 *
 * <p>Two capabilities:
 * <ul>
 *   <li>{@link #walk} follows a structural JMESPath over a starting shape and reports the
 *       resolved leaf value shape, whether it is the root, and how many array (projection or
 *       flatten) levels were traversed.</li>
 *   <li>{@link #inferByName} matches the members of a structure to a resource's identifier or
 *       property names, honoring {@code @resourceIdentifier}, {@code @property}, and
 *       {@code @notProperty}, mirroring the standard lifecycle resolution.</li>
 * </ul>
 *
 * <p>Only the structural JMESPath subset is supported (field, subexpression, projection
 * {@code [*]}, flatten {@code []}, current node). Callers reject unsupported expressions
 * before walking.
 */
final class ResourceLifecycleResolver {

    private ResourceLifecycleResolver() {}

    /**
     * Walks a structural JMESPath over a starting shape.
     *
     * @param model The model.
     * @param start The starting structure (operation input or output).
     * @param expr The parsed structural JMESPath.
     * @return The resolved leaf shape and metadata, or an error.
     */
    static PathResult walk(Model model, Shape start, JmespathExpression expr) {
        boolean root = expr instanceof CurrentExpression;
        Result r = expr.accept(new ShapeWalkVisitor(model, new Type(start, new ArrayList<>())));
        if (r.error != null) {
            return PathResult.error(r.error);
        }
        return PathResult.of(r.type.shape, root, r.type.arrays);
    }

    /**
     * Resolves the terminal member that a structural path designates, if any.
     *
     * <p>Handles a pure chain of field accesses (for example {@code output.fooId}, which lands on
     * the {@code fooId} member of whatever {@code output} targets). Returns {@code null} for any
     * path that is not a pure field chain (projections, flatten, or the root {@code @}), because
     * those designate a value inside a list element or the whole structure rather than a single
     * top-level member, and for a chain whose intermediate segment is not a nested structure or
     * whose final field does not exist. Unlike {@link #walk}, this does not track cardinality; it
     * answers only "which member does this path point at".
     *
     * @param model The model.
     * @param start The starting structure (operation input or output).
     * @param expr The parsed structural JMESPath.
     * @return The terminal member, or null if the path does not designate one.
     */
    static MemberShape resolveTerminalMember(Model model, Shape start, JmespathExpression expr) {
        List<String> fields = new ArrayList<>();
        if (!collectFieldChain(expr, fields) || fields.isEmpty()) {
            return null;
        }
        Shape current = start;
        for (int i = 0; i < fields.size(); i++) {
            if (!(current instanceof StructureShape) && !(current instanceof UnionShape)) {
                return null;
            }
            MemberShape member = current.getMember(fields.get(i)).orElse(null);
            if (member == null) {
                return null;
            }
            if (i == fields.size() - 1) {
                return member;
            }
            // Descend into an intermediate member's target, which must be a plain (non-array) shape.
            Shape target = model.expectShape(member.getTarget());
            if (target instanceof ListShape) {
                return null;
            }
            current = target;
        }
        return null;
    }

    /**
     * Infers which members of a structure provide the resource's identifiers or properties.
     *
     * @param resource The resource being bound.
     * @param element The structure to match members against.
     * @param kind Whether to match identifier names or property names.
     * @return The matched and unmatched members.
     */
    static InferenceResult inferByName(ResourceShape resource, StructureShape element, BindingKind kind) {
        InferenceResult result = new InferenceResult();
        for (MemberShape member : element.members()) {
            if (member.hasTrait(NotPropertyTrait.ID)) {
                continue;
            }
            String boundName = boundName(member, kind);
            boolean matched = kind == BindingKind.IDENTIFIER
                    ? resource.getIdentifiers().containsKey(boundName)
                    : resource.getProperties().containsKey(boundName);
            if (matched) {
                result.matched.put(boundName, member);
            } else {
                result.unmatched.add(member);
            }
        }
        return result;
    }

    // Whether a member set is being matched against a resource's identifiers or its properties.
    enum BindingKind {
        IDENTIFIER, PROPERTY
    }

    // The result of walking a structural path over a starting shape.
    static final class PathResult {
        // The leaf value shape the path resolves to, with array levels unwrapped, or null on error.
        final Shape leaf;
        // True if the path is the bare current node (resolves to the starting shape/root).
        final boolean root;
        // The ordered identities of the lists the path iterates through (outermost first), forming a
        // cardinality signature. Two paths correlate element-for-element iff their signatures are equal.
        // Empty for a scalar (non-array) leaf.
        final List<ShapeId> arrays;
        // Number of array levels (projections or flattens) traversed; equal to arrays.size().
        final int arrayDepth;
        // Non-null when a segment could not be resolved against the model.
        final String error;

        private PathResult(Shape leaf, boolean root, List<ShapeId> arrays, String error) {
            this.leaf = leaf;
            this.root = root;
            this.arrays = arrays == null ? Collections.emptyList() : Collections.unmodifiableList(arrays);
            this.arrayDepth = this.arrays.size();
            this.error = error;
        }

        static PathResult of(Shape leaf, boolean root, List<ShapeId> arrays) {
            return new PathResult(leaf, root, arrays, null);
        }

        static PathResult error(String error) {
            return new PathResult(null, false, null, error);
        }
    }

    // The result of inferring resource members from a structure by name.
    static final class InferenceResult {
        // Matched identifier or property name to the structure member that provides it.
        final Map<String, MemberShape> matched = new LinkedHashMap<>();
        // Members that matched no identifier or property name and are not @notProperty.
        final List<MemberShape> unmatched = new ArrayList<>();
    }

    private static String boundName(MemberShape member, BindingKind kind) {
        if (kind == BindingKind.IDENTIFIER) {
            return member.getTrait(ResourceIdentifierTrait.class)
                    .map(ResourceIdentifierTrait::getValue)
                    .orElseGet(member::getMemberName);
        }
        return member.getTrait(PropertyTrait.class)
                .flatMap(PropertyTrait::getName)
                .orElseGet(member::getMemberName);
    }

    // Flattens a pure field chain (fields joined by subexpressions) into ordered field names.
    // Returns false if any node is not a field or subexpression (projection, flatten, current, ...).
    private static boolean collectFieldChain(JmespathExpression expr, List<String> fields) {
        if (expr instanceof FieldExpression) {
            fields.add(((FieldExpression) expr).getName());
            return true;
        } else if (expr instanceof Subexpression) {
            Subexpression sub = (Subexpression) expr;
            return collectFieldChain(sub.getLeft(), fields) && collectFieldChain(sub.getRight(), fields);
        }
        return false;
    }

    private static Result resolveField(Model model, FieldExpression field, Type in) {
        if (!in.arrays.isEmpty()) {
            return Result.err("cannot access field `" + field.getName() + "` on an array");
        }
        Shape shape = in.shape;
        if (shape instanceof StructureShape || shape instanceof UnionShape) {
            MemberShape member = shape.getMember(field.getName()).orElse(null);
            if (member == null) {
                return Result.err("no member `" + field.getName() + "` on `" + shape.getId() + "`");
            }
            return Result.ok(normalize(model, member.getTarget()));
        } else if (shape instanceof MapShape) {
            // Field access on a map resolves to the map value type.
            return Result.ok(normalize(model, ((MapShape) shape).getValue().getTarget()));
        }
        return Result.err("cannot access field `" + field.getName() + "` on `" + shape.getId() + "`");
    }

    // Unwraps list levels into an arrays signature so the returned shape is never itself a list.
    private static Type normalize(Model model, ShapeId targetId) {
        Shape shape = model.expectShape(targetId);
        List<ShapeId> arrays = new ArrayList<>();
        while (shape instanceof ListShape) {
            arrays.add(shape.getId());
            shape = model.expectShape(((ListShape) shape).getMember().getTarget());
        }
        return new Type(shape, arrays);
    }

    // Internal descriptor: a value of "shape" wrapped in the listed arrays (outermost first).
    private static final class Type {
        final Shape shape;
        final List<ShapeId> arrays;

        Type(Shape shape, List<ShapeId> arrays) {
            this.shape = shape;
            this.arrays = arrays;
        }
    }

    // The outcome of a single walk step: either a resolved Type or an error message.
    private static final class Result {
        final Type type;
        final String error;

        private Result(Type type, String error) {
            this.type = type;
            this.error = error;
        }

        static Result ok(Type type) {
            return new Result(type, null);
        }

        static Result err(String error) {
            return new Result(null, error);
        }
    }

    // Walks a structural JMESPath over a starting shape, tracking the resolved value shape and the
    // stack of array (projection/flatten) identities. Only the structural subset is supported; every
    // other node type resolves to an error (callers reject unsupported expressions before walking).
    private static final class ShapeWalkVisitor implements ExpressionVisitor<Result> {
        private final Model model;
        private final Type in;

        ShapeWalkVisitor(Model model, Type in) {
            this.model = model;
            this.in = in;
        }

        private Result walkInto(JmespathExpression expr, Type input) {
            return expr.accept(new ShapeWalkVisitor(model, input));
        }

        @Override
        public Result visitCurrentNode(CurrentExpression expression) {
            return Result.ok(in);
        }

        @Override
        public Result visitField(FieldExpression expression) {
            return resolveField(model, expression, in);
        }

        @Override
        public Result visitSubexpression(Subexpression expression) {
            Result left = expression.getLeft().accept(this);
            if (left.error != null) {
                return left;
            }
            return walkInto(expression.getRight(), left.type);
        }

        @Override
        public Result visitProjection(ProjectionExpression expression) {
            Result left = expression.getLeft().accept(this);
            if (left.error != null) {
                return left;
            }
            if (left.type.arrays.isEmpty()) {
                return Result.err("projection `[*]` applied to a non-array");
            }
            // Iterate the outermost array; remember its identity to re-wrap the projected result.
            List<ShapeId> outer = left.type.arrays;
            ShapeId iterated = outer.get(0);
            Type element = new Type(left.type.shape, new ArrayList<>(outer.subList(1, outer.size())));
            Result right = walkInto(expression.getRight(), element);
            if (right.error != null) {
                return right;
            }
            List<ShapeId> arrays = new ArrayList<>();
            arrays.add(iterated);
            arrays.addAll(right.type.arrays);
            return Result.ok(new Type(right.type.shape, arrays));
        }

        @Override
        public Result visitFlatten(FlattenExpression expression) {
            Result inner = expression.getExpression().accept(this);
            if (inner.error != null) {
                return inner;
            }
            if (inner.type.arrays.isEmpty()) {
                return Result.err("flatten `[]` applied to a non-array");
            }
            // Flatten merges one level of nesting into the outer array. A single-level array is
            // unchanged; nested arrays collapse the second level into the first (outer identity kept).
            List<ShapeId> arrays = new ArrayList<>(inner.type.arrays);
            if (arrays.size() >= 2) {
                arrays.remove(1);
            }
            return Result.ok(new Type(inner.type.shape, arrays));
        }

        private Result unsupported() {
            return Result.err("unsupported expression");
        }

        @Override
        public Result visitComparator(ComparatorExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitExpressionType(ExpressionTypeExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitFunction(FunctionExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitIndex(IndexExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitLiteral(LiteralExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitMultiSelectList(MultiSelectListExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitMultiSelectHash(MultiSelectHashExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitAnd(AndExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitOr(OrExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitNot(NotExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitFilterProjection(FilterProjectionExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitObjectProjection(ObjectProjectionExpression expression) {
            return unsupported();
        }

        @Override
        public Result visitSlice(SliceExpression expression) {
            return unsupported();
        }
    }
}
