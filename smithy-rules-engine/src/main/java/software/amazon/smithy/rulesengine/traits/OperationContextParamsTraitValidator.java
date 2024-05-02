/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.traits;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
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
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Validates {@link OperationContextParamsTrait} traits.
 */
@SmithyUnstableApi
public final class OperationContextParamsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ContextIndex index = ContextIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operationShape : model.getOperationShapes()) {
            Map<String, OperationContextParamDefinition> definitionMap = index.getOperationContextParams(operationShape)
                    .map(OperationContextParamsTrait::getParameters)
                    .orElse(Collections.emptyMap());
            for (Map.Entry<String, OperationContextParamDefinition> entry : definitionMap.entrySet()) {

                try {
                    JmespathExpression path = JmespathExpression.parse(entry.getValue().getPath());
                    StructureShape input = OperationIndex.of(model).expectInputShape(operationShape);
                    LinterResult linterResult = path.lint(createCurrentNodeFromShape(input, model));

                    if (!linterResult.getProblems().isEmpty()) {
                        events.add(error(operationShape,
                                String.format("The operation `%s` is marked with `%s` which contains a "
                                                + "path `%s` with an invalid JMESPath path `%s`: %s.",
                                        operationShape.getId(),
                                        OperationContextParamsTrait.ID.toString(),
                                        entry.getKey(),
                                        entry.getValue().getPath(),
                                        linterResult.getProblems().stream()
                                                .map(p -> "'" + p.message + "'")
                                                .collect(Collectors.joining(", "))
                                )));
                    }

                    List<String> unsupportedExpressions = path.accept(new UnsupportedJmesPathVisitor());
                    if (!unsupportedExpressions.isEmpty()) {
                        events.add(error(operationShape,
                                String.format("The operation `%s` is marked with `%s` which contains a "
                                                + "key `%s` with a JMESPath path `%s` with "
                                                + "unsupported expressions: %s.",
                                        operationShape.getId(),
                                        OperationContextParamsTrait.ID.toString(),
                                        entry.getKey(),
                                        entry.getValue().getPath(),
                                        unsupportedExpressions.stream()
                                                .map(e -> "'" + e + "'")
                                                .collect(Collectors.joining(", "))
                                )));
                    }
                } catch (JmespathException e) {
                    events.add(error(operationShape,
                            String.format("The operation `%s` is marked with `%s` which contains a "
                                            + "key `%s` with an unparseable JMESPath path `%s`: %s.",
                                    operationShape.getId(),
                                    OperationContextParamsTrait.ID.toString(),
                                    entry.getKey(),
                                    entry.getValue().getPath(),
                                    e.getMessage()
                            )));
                }
            }
        }
        return events;
    }

    private LiteralExpression createCurrentNodeFromShape(Shape shape, Model model) {
        return shape == null
                ? LiteralExpression.ANY
                : new LiteralExpression(shape.accept(new ModelRuntimeTypeGenerator(model)));
    }

    private static final class UnsupportedJmesPathVisitor implements ExpressionVisitor<List<String>> {

        @Override
        public List<String> visitComparator(ComparatorExpression expression) {
            return ListUtils.of("comparator");
        }

        @Override
        public List<String> visitCurrentNode(CurrentExpression expression) {
            return ListUtils.of("current node");
        }

        @Override
        public List<String> visitExpressionType(ExpressionTypeExpression expression) {
            return expression.getExpression().accept(this);
        }

        @Override
        public List<String> visitFlatten(FlattenExpression expression) {
            return ListUtils.of("flatten");
        }

        @Override
        public List<String> visitFunction(FunctionExpression expression) {
            if (expression.getName().equals("keys")) {
                return expression.getArguments().stream()
                        .map(e -> e.accept(this))
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            } else {
                return ListUtils.of("`" + expression.getName() + "` function");
            }
        }

        @Override
        public List<String> visitField(FieldExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitIndex(IndexExpression expression) {
            return ListUtils.of("index");
        }

        @Override
        public List<String> visitLiteral(LiteralExpression expression) {
            return ListUtils.of("literal");
        }

        @Override
        public List<String> visitMultiSelectList(MultiSelectListExpression expression) {
            return ListUtils.of("multiselect list");
        }

        @Override
        public List<String> visitMultiSelectHash(MultiSelectHashExpression expression) {
            return ListUtils.of("multiselect hash");
        }

        @Override
        public List<String> visitAnd(AndExpression expression) {
            return ListUtils.of("and");
        }

        @Override
        public List<String> visitOr(OrExpression expression) {
            return ListUtils.of("or");
        }

        @Override
        public List<String> visitNot(NotExpression expression) {
            return ListUtils.of("not");
        }

        @Override
        public List<String> visitProjection(ProjectionExpression expression) {
            return Collections.emptyList();
        }

        @Override
        public List<String> visitFilterProjection(FilterProjectionExpression expression) {
            return ListUtils.of("filter projection");
        }

        @Override
        public List<String> visitObjectProjection(ObjectProjectionExpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }

        @Override
        public List<String> visitSlice(SliceExpression expression) {
            return ListUtils.of("slice");
        }

        @Override
        public List<String> visitSubexpression(Subexpression expression) {
            List<String> unsupported = new ArrayList<>();
            unsupported.addAll(expression.getLeft().accept(this));
            unsupported.addAll(expression.getRight().accept(this));
            return Collections.unmodifiableList(unsupported);
        }
    }

    /**
     * This class is duplicated from
     * smithy-waiters/src/main/java/software/amazon/smithy/waiters/ModelRuntimeTypeGenerator.java
     *
     * It is duplicated here to avoid taking a dependency on smithy-waiters.
     */
    private static final class ModelRuntimeTypeGenerator implements ShapeVisitor<Object> {

        private final Model model;
        private Set<MemberShape> visited = new HashSet<>();

        ModelRuntimeTypeGenerator(Model model) {
            this.model = model;
        }

        @Override
        public Object blobShape(BlobShape shape) {
            return "blob";
        }

        @Override
        public Object booleanShape(BooleanShape shape) {
            return true;
        }

        @Override
        public Object byteShape(ByteShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object shortShape(ShortShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object integerShape(IntegerShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object longShape(LongShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object floatShape(FloatShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object doubleShape(DoubleShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object bigIntegerShape(BigIntegerShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object bigDecimalShape(BigDecimalShape shape) {
            return computeRange(shape);
        }

        @Override
        public Object documentShape(DocumentShape shape) {
            return LiteralExpression.ANY;
        }

        @Override
        public Object stringShape(StringShape shape) {
            // Create a random string that does not exceed or go under the length trait.
            int chars = computeLength(shape);

            // Fill a string with "a"'s up to chars.
            return new String(new char[chars]).replace("\0", "a");
        }

        @Override
        public Object listShape(ListShape shape) {
            return withCopiedVisitors(() -> {
                int size = computeLength(shape);
                List<Object> result = new ArrayList<>(size);
                Object memberValue = shape.getMember().accept(this);
                if (memberValue != null) {
                    for (int i = 0; i < size; i++) {
                        result.add(memberValue);
                    }
                }
                return result;
            });
        }

        // Visits members and mutates a copy of the current set of visited
        // shapes rather than a shared set. This allows a shape to be used
        // multiple times in the closure of a single shape without causing the
        // reuse of the shape to always be assumed to be a recursive type.
        private Object withCopiedVisitors(Supplier<Object> supplier) {
            // Account for recursive shapes at the current
            Set<MemberShape> visitedCopy = new HashSet<>(visited);
            Object result = supplier.get();
            visited = visitedCopy;
            return result;
        }

        @Override
        public Object mapShape(MapShape shape) {
            return withCopiedVisitors(() -> {
                int size = computeLength(shape);
                Map<String, Object> result = new HashMap<>();
                String key = (String) shape.getKey().accept(this);
                Object memberValue = shape.getValue().accept(this);
                for (int i = 0; i < size; i++) {
                    result.put(key + i, memberValue);
                }
                return result;
            });
        }

        @Override
        public Object structureShape(StructureShape shape) {
            return structureOrUnion(shape);
        }

        @Override
        public Object unionShape(UnionShape shape) {
            return structureOrUnion(shape);
        }

        private Object structureOrUnion(Shape shape) {
            return withCopiedVisitors(() -> {
                Map<String, Object> result = new LinkedHashMap<>();
                for (MemberShape member : shape.members()) {
                    Object memberValue = member.accept(this);
                    result.put(member.getMemberName(), memberValue);
                }
                return result;
            });
        }

        @Override
        public Object memberShape(MemberShape shape) {
            // Account for recursive shapes.
            // A false return value means it was in the set.
            if (!visited.add(shape)) {
                return LiteralExpression.ANY;
            }

            return model.getShape(shape.getTarget())
                    .map(target -> target.accept(this))
                    // Rather than fail on broken models during waiter validation,
                    // return an ANY to get *some* validation.
                    .orElse(LiteralExpression.ANY);
        }

        @Override
        public Object timestampShape(TimestampShape shape) {
            return LiteralExpression.NUMBER;
        }

        @Override
        public Object operationShape(OperationShape shape) {
            throw new UnsupportedOperationException(shape.toString());
        }

        @Override
        public Object resourceShape(ResourceShape shape) {
            throw new UnsupportedOperationException(shape.toString());
        }

        @Override
        public Object serviceShape(ServiceShape shape) {
            throw new UnsupportedOperationException(shape.toString());
        }

        private int computeLength(Shape shape) {
            // Create a random string that does not exceed or go under the length trait.
            int chars = 2;

            if (shape.hasTrait(LengthTrait.class)) {
                LengthTrait trait = shape.expectTrait(LengthTrait.class);
                if (trait.getMin().isPresent()) {
                    chars = Math.max(chars, trait.getMin().get().intValue());
                }
                if (trait.getMax().isPresent()) {
                    chars = Math.min(chars, trait.getMax().get().intValue());
                }
            }

            return chars;
        }

        private double computeRange(Shape shape) {
            // Create a random string that does not exceed or go under the range trait.
            double i = 8;

            if (shape.hasTrait(RangeTrait.class)) {
                RangeTrait trait = shape.expectTrait(RangeTrait.class);
                if (trait.getMin().isPresent()) {
                    i = Math.max(i, trait.getMin().get().doubleValue());
                }
                if (trait.getMax().isPresent()) {
                    i = Math.min(i, trait.getMax().get().doubleValue());
                }
            }

            return i;
        }
    }
}
