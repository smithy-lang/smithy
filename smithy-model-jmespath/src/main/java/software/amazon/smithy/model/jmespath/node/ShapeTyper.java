/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.jmespath.node;

import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.type.ArrayType;
import software.amazon.smithy.jmespath.type.BooleanType;
import software.amazon.smithy.jmespath.type.MapType;
import software.amazon.smithy.jmespath.type.NumberType;
import software.amazon.smithy.jmespath.type.ObjectType;
import software.amazon.smithy.jmespath.type.StringType;
import software.amazon.smithy.jmespath.type.Type;
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
import software.amazon.smithy.model.shapes.NumberShape;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Generates fake data from a modeled shape for static JMESPath analysis.
 */
final class ShapeTyper implements ShapeVisitor<Type> {

    private final Model model;
    private Set<MemberShape> visited = new HashSet<>();

    ShapeTyper(Model model) {
        this.model = model;
    }

    @Override
    public Type blobShape(BlobShape shape) {
        return StringType.INSTANCE;
    }

    @Override
    public Type booleanShape(BooleanShape shape) {
        return BooleanType.INSTANCE;
    }

    @Override
    public Type byteShape(ByteShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type shortShape(ShortShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type integerShape(IntegerShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type longShape(LongShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type floatShape(FloatShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type doubleShape(DoubleShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type bigIntegerShape(BigIntegerShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type bigDecimalShape(BigDecimalShape shape) {
        return NumberType.INSTANCE;
    }

    @Override
    public Type documentShape(DocumentShape shape) {
        return Type.anyType();
    }

    @Override
    public Type stringShape(StringShape shape) {
        return StringType.INSTANCE;
    }

    @Override
    public Type listShape(ListShape shape) {
        return withCopiedVisitors(() -> {
            Type memberType = shape.getMember().accept(this);
            return new ArrayType(memberType);
        });
    }

    // Visits members and mutates a copy of the current set of visited
    // shapes rather than a shared set. This allows a shape to be used
    // multiple times in the closure of a single shape without causing the
    // reuse of the shape to always be assumed to be a recursive type.
    private Type withCopiedVisitors(Supplier<Type> supplier) {
        // Account for recursive shapes at the current
        Set<MemberShape> visitedCopy = new HashSet<>(visited);
        Type result = supplier.get();
        visited = visitedCopy;
        return result;
    }

    @Override
    public Type mapShape(MapShape shape) {
        return withCopiedVisitors(() -> {
            Type keyType = shape.getKey().accept(this);
            Type valueType = shape.getValue().accept(this);
            return new MapType(keyType, valueType);
        });
    }

    @Override
    public Type structureShape(StructureShape shape) {
        return structureOrUnion(shape);
    }

    @Override
    public Type unionShape(UnionShape shape) {
        return structureOrUnion(shape);
    }

    private Type structureOrUnion(Shape shape) {
        return withCopiedVisitors(() -> {
            Map<String, Type> result = new LinkedHashMap<>();
            for (MemberShape member : shape.members()) {
                Type memberType = member.accept(this);
                result.put(member.getMemberName(), memberType);
            }
            return new ObjectType(result);
        });
    }

    @Override
    public Type memberShape(MemberShape shape) {
        // Account for recursive shapes.
        // A false return value means it was in the set.
        // TODO: Can Type represent recursive types?
        if (!visited.add(shape)) {
            return Type.anyType();
        }

        return model.getShape(shape.getTarget())
                .map(target -> target.accept(this))
                // Rather than fail on broken models during waiter validation,
                // return an ANY to get *some* validation.
                .orElse(Type.anyType());
    }

    @Override
    public Type timestampShape(TimestampShape shape) {
        return new NumberType();
    }

    @Override
    public Type operationShape(OperationShape shape) {
        throw new UnsupportedOperationException(shape.toString());
    }

    @Override
    public Type resourceShape(ResourceShape shape) {
        throw new UnsupportedOperationException(shape.toString());
    }

    @Override
    public Type serviceShape(ServiceShape shape) {
        throw new UnsupportedOperationException(shape.toString());
    }
}
