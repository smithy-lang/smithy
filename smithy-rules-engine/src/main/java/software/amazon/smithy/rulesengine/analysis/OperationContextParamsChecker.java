/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.LinterResult;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
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
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.traits.OperationContextParamDefinition;

public final class OperationContextParamsChecker {

    private OperationContextParamsChecker() {

    }

    public static LinterResult lint(
            OperationContextParamDefinition paramDefinition,
            OperationShape operationShape,
            Model model
    ) {
        JmespathExpression path = JmespathExpression.parse(paramDefinition.getPath());
        StructureShape input = OperationIndex.of(model).expectInputShape(operationShape);
        return path.lint(createCurrentNodeFromShape(input, model));
    }

    public static Optional<ParameterType> inferParameterType(
            OperationContextParamDefinition paramDefinition,
            OperationShape operationShape,
            Model model
    ) {
        RuntimeType runtimeType = lint(paramDefinition, operationShape, model).getReturnType();
        switch (runtimeType) {
            case BOOLEAN:
                return Optional.of(ParameterType.BOOLEAN);
            case STRING:
                return Optional.of(ParameterType.STRING);
            case ARRAY:
                return Optional.of(ParameterType.STRING_ARRAY);
            default:
                return Optional.empty();
        }
    }

    private static LiteralExpression createCurrentNodeFromShape(Shape shape, Model model) {
        return shape == null
                ? LiteralExpression.ANY
                : new LiteralExpression(shape.accept(new ModelRuntimeTypeGenerator(model)));
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
