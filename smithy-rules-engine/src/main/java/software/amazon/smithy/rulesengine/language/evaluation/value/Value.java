/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.evaluation.value;

import static java.lang.String.format;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An abstract representing a typed value.
 */
@SmithyUnstableApi
public abstract class Value implements FromSourceLocation, ToNode {
    private SourceLocation sourceLocation;

    Value(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public abstract Type getType();

    /**
     * Creates a {@link Value} of a specific type from the given Node information.
     *
     * @param source the node to deserialize.
     * @return the created Value.
     */
    public static Value fromNode(Node source) {
        Value value = source.accept(new NodeVisitor<Value>() {
            @Override
            public Value arrayNode(ArrayNode node) {
                return new ArrayValue(node.getElementsAs(Value::fromNode));
            }

            @Override
            public Value booleanNode(BooleanNode node) {
                return booleanValue(node.getValue());
            }

            @Override
            public Value nullNode(NullNode node) {
                return emptyValue();
            }

            @Override
            public Value numberNode(NumberNode node) {
                int nodeValue = node.getValue().intValue();
                if (node.isFloatingPointNumber() || nodeValue < 0) {
                    throw new RuntimeException("only integers >=0 are supported");
                }
                return Value.integerValue(nodeValue);
            }

            @Override
            public Value objectNode(ObjectNode node) {
                Map<Identifier, Value> out = new LinkedHashMap<>();
                node.getMembers().forEach((name, member) -> out.put(Identifier.of(name), Value.fromNode(member)));
                return Value.recordValue(out);
            }

            @Override
            public Value stringNode(StringNode node) {
                return Value.stringValue(node.getValue());
            }
        });
        value.sourceLocation = source.getSourceLocation();
        return value;
    }

    public boolean isEmpty() {
        return false;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return Optional.ofNullable(sourceLocation).orElse(SourceLocation.none());
    }

    /**
     * Creates an {@link ArrayValue} from a list of values.
     *
     * @param value the list of values for the array.
     * @return returns the created ArrayValue.
     */
    public static ArrayValue arrayValue(List<Value> value) {
        return new ArrayValue(value);
    }

    /**
     * Creates an {@link BooleanValue} from a boolean.
     *
     * @param value the value for the boolean.
     * @return returns the created BooleanValue.
     */
    public static BooleanValue booleanValue(boolean value) {
        return new BooleanValue(value);
    }

    /**
     * Creates an {@link EmptyValue}.
     *
     * @return returns the created EmptyValue.
     */
    public static EmptyValue emptyValue() {
        return new EmptyValue();
    }

    /**
     * Creates an {@link EndpointValue} from a node.
     *
     * @param source the node to create an endpoint from.
     * @return returns the created EndpointValue.
     */
    public static EndpointValue endpointValue(Node source) {
        return EndpointValue.fromNode(source);
    }

    /**
     * Creates an {@link IntegerValue} from an integer.
     *
     * @param value the value for the integer.
     * @return returns the created IntegerValue.
     */
    public static IntegerValue integerValue(int value) {
        return new IntegerValue(value);
    }

    /**
     * Creates an {@link RecordValue} from a map of identifiers to values.
     *
     * @param value the map to create a record from.
     * @return returns the created RecordValue.
     */
    public static RecordValue recordValue(Map<Identifier, Value> value) {
        return new RecordValue(value);
    }

    /**
     * Creates an {@link StringValue} from a string.
     *
     * @param value the value for the string.
     * @return returns the created StringValue.
     */
    public static StringValue stringValue(String value) {
        return new StringValue(value);
    }

    /**
     * Returns the current value as an {@link ArrayValue}, throwing
     * {@link RuntimeException} when the value is the wrong type.
     *
     * @return returns an array value.
     */
    public ArrayValue expectArrayValue() {
        throw throwTypeMismatch("ArrayType");
    }

    /**
     * Returns the current value as an {@link ArrayValue}, throwing
     * {@link RuntimeException} when the value is the wrong type.
     *
     * @return returns a boolean value.
     */
    public BooleanValue expectBooleanValue() {
        throw throwTypeMismatch("BooleanType");
    }

    /**
     * Returns the current value as an {@link ArrayValue}, throwing
     * {@link RuntimeException} when the value is the wrong type.
     *
     * @return returns an endpoint value
     */
    public EndpointValue expectEndpointValue() {
        throw throwTypeMismatch("EndpointType[]");
    }

    /**
     * Returns the current value as an {@link ArrayValue}, throwing
     * {@link RuntimeException} when the value is the wrong type.
     *
     * @return returns an integer value.
     */
    public IntegerValue expectIntegerValue() {
        throw throwTypeMismatch("IntegerType");
    }

    /**
     * Returns the current value as an {@link ArrayValue}, throwing
     * {@link RuntimeException} when the value is the wrong type.
     *
     * @return returns a record value.
     */
    public RecordValue expectRecordValue() {
        throw throwTypeMismatch("RecordType");
    }

    /**
     * Returns the current value as an {@link ArrayValue}, throwing
     * {@link RuntimeException} when the value is the wrong type.
     *
     * @return returns a string value.
     */
    public StringValue expectStringValue() {
        throw throwTypeMismatch("StringType");
    }

    private RuntimeException throwTypeMismatch(String expectedType) {
        return new RuntimeException(format("Expected `%s` but was `%s` with value: `%s`",
                expectedType, getType(), this));
    }
}
