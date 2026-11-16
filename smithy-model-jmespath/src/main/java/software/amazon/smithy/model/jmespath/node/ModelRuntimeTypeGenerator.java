/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmespath.node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.model.Model;
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

/**
 * Generates fake data from a modeled shape for static JMESPath analysis.
 *
 * <p>The generated sample is bounded. Strings, lists, and maps are capped in
 * length, the number of values in the sample (counting each reuse of a shared
 * value) is capped by a node budget, and nesting is capped in depth. Values
 * past these limits become {@link LiteralExpression#ANY}.
 */
final class ModelRuntimeTypeGenerator implements ShapeVisitor<Object> {

    static final int MAX_STRING_SAMPLE_LENGTH = 16;
    static final int MAX_COLLECTION_SAMPLE_LENGTH = 1000;
    static final long MAX_SAMPLE_NODES = 100_000L;
    static final int MAX_SAMPLE_DEPTH = 256;

    private final Model model;
    private Set<MemberShape> visited = new HashSet<>();
    private long nodes;
    private int depth;

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
        int chars = computeLength(shape, MAX_STRING_SAMPLE_LENGTH);

        // Fill a string with "a"'s up to chars.
        return new String(new char[chars]).replace("\0", "a");
    }

    @Override
    public Object listShape(ListShape shape) {
        return withCopiedVisitors(() -> {
            int size = computeLength(shape, MAX_COLLECTION_SAMPLE_LENGTH);
            long before = nodes;
            Object memberValue = shape.getMember().accept(this);
            int count = affordableElements(size, nodes - before);
            List<Object> result = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                result.add(memberValue);
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
            int size = computeLength(shape, MAX_COLLECTION_SAMPLE_LENGTH);
            long before = nodes;
            Object keyValue = shape.getKey().accept(this);
            String key = keyValue instanceof String ? (String) keyValue : "key";
            Object memberValue = shape.getValue().accept(this);
            int count = affordableElements(size, nodes - before);
            Map<String, Object> result = new HashMap<>();
            for (int i = 0; i < count; i++) {
                result.put(key + i, memberValue);
            }
            return result;
        });
    }

    // Returns how many of the wanted elements to emit and charges the node budget for them.
    // The first element was already charged while it was built. The linter walks every
    // element, so each further copy costs the same number of nodes as the first one.
    private int affordableElements(int wanted, long elementCost) {
        if (wanted == 0) {
            return 0;
        }
        long cost = Math.max(1, elementCost);
        int copies = (int) Math.min(wanted - 1, Math.max(0, (MAX_SAMPLE_NODES - nodes) / cost));
        nodes += copies * cost;
        return 1 + copies;
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
        // Stop expanding once the node budget is spent.
        if (nodes >= MAX_SAMPLE_NODES) {
            return LiteralExpression.ANY;
        }

        // Charge every value before any early return so recursion leaves are counted.
        nodes++;

        // Account for recursive shapes.
        // A false return value means it was in the set.
        if (!visited.add(shape)) {
            return LiteralExpression.ANY;
        }

        if (depth >= MAX_SAMPLE_DEPTH) {
            return LiteralExpression.ANY;
        }

        depth++;
        try {
            return model.getShape(shape.getTarget())
                    .map(target -> target.accept(this))
                    // Rather than fail on broken models during waiter validation,
                    // return an ANY to get *some* validation.
                    .orElse(LiteralExpression.ANY);
        } finally {
            depth--;
        }
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

    private int computeLength(Shape shape, int cap) {
        // Create a length that does not exceed or go under the length trait, clamped to [0, cap].
        long length = 2;

        if (shape.hasTrait(LengthTrait.ID)) {
            LengthTrait trait = shape.expectTrait(LengthTrait.class);
            if (trait.getMin().isPresent()) {
                length = Math.max(length, trait.getMin().get());
            }
            if (trait.getMax().isPresent()) {
                length = Math.min(length, trait.getMax().get());
            }
        }

        return (int) Math.max(0, Math.min(length, cap));
    }

    private double computeRange(Shape shape) {
        // Create a random string that does not exceed or go under the range trait.
        double i = 8;

        if (shape.hasTrait(RangeTrait.ID)) {
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
