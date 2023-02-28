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

package software.amazon.smithy.rulesengine.language.eval.value;

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
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An abstract representing a typed value.
 */
@SmithyUnstableApi
public abstract class Value implements FromSourceLocation, ToNode {
    private SourceLocation sourceLocation;

    public Value(SourceLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

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
                if (!node.isNaturalNumber()) {
                    throw new RuntimeException("only integers >=0 are supported");
                }
                int nodeValue = node.getValue().intValue();
                if (nodeValue < 0) {
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

    public abstract Type getType();

    public boolean isEmpty() {
        return false;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return Optional.ofNullable(sourceLocation).orElse(SourceLocation.none());
    }

    public static ArrayValue arrayValue(List<Value> value) {
        return new ArrayValue(value);
    }

    public static BooleanValue booleanValue(boolean value) {
        return new BooleanValue(value);
    }

    public static EmptyValue emptyValue() {
        return new EmptyValue();
    }

    public static EndpointValue endpointValue(Node source) {
        return EndpointValue.fromNode(source);
    }

    public static IntegerValue integerValue(int value) {
        return new IntegerValue(value);
    }

    public static RecordValue recordValue(Map<Identifier, Value> value) {
        return new RecordValue(value);
    }

    public static StringValue stringValue(String value) {
        return new StringValue(value);
    }

    public ArrayValue expectArrayValue() {
        throw new RuntimeException("Expected array, found " + this);
    }

    public BooleanValue expectBooleanValue() {
        throw new RuntimeException("Expected bool but was: " + this);
    }

    public EndpointValue expectEndpointValue() {
        throw new RuntimeException("Expected endpoint, found " + this);
    }

    public IntegerValue expectIntegerValue() {
        throw new RuntimeException("Expected int, found " + this);
    }

    public RecordValue expectRecordValue() {
        throw new RuntimeException("Expected object but was: " + this);
    }

    public StringValue expectStringValue() {
        throw new RuntimeException("Expected string but was: " + this);
    }

}
