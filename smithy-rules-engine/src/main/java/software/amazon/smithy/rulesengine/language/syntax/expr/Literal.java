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

package software.amazon.smithy.rulesengine.language.syntax.expr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.eval.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.util.SourceLocationUtils;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.visit.TemplateVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Literals allow rules and properties to define arbitrarily nested JSON structures (e.g.for properties)
 * <p>
 * They support template strings, but _do not_ support template objects since that creates ambiguity. {@link Template}s
 * are a basic example of literals–literal strings. Literals can also be booleans, objects, integers or tuples.
 */
@SmithyUnstableApi
public final class Literal extends Expression {

    private final ILiteral source;

    private Literal(ILiteral source, FromSourceLocation sourceLocation) {
        super(sourceLocation.getSourceLocation());
        this.source = source;
    }

    /**
     * Constructs a tuple literal of values.
     *
     * @param values the values.
     * @return the tuple literal.
     */
    public static Literal tuple(List<Literal> values) {
        return new Literal(new Tuple(values), SourceLocationUtils.javaLocation());
    }

    /**
     * Constructs a record literal of values.
     *
     * @param record a map of values to be converted to a record.
     * @return the record literal.
     */
    public static Literal record(Map<Identifier, Literal> record) {
        return new Literal(new Record(record), SourceLocationUtils.javaLocation());
    }

    /**
     * Constructs a string literal from a {@link Template} value.
     *
     * @param value the template value.
     * @return the string literal.
     */
    public static Literal string(Template value) {
        return new Literal(new String(value), SourceLocationUtils.javaLocation());
    }

    /**
     * Constructs an integer literal from an integer value.
     *
     * @param value the integer value.
     * @return the integer literal.
     */
    public static Literal integer(int value) {
        return new Literal(new Integer(Node.from(value)), SourceLocationUtils.javaLocation());
    }

    /**
     * Constructs a bool literal from a boolean value.
     *
     * @param value the boolean value.
     * @return the bool literal.
     */
    public static Literal bool(boolean value) {
        return new Literal(new Bool(Node.from(value)), SourceLocationUtils.javaLocation());
    }

    /**
     * Constructs a literal from a {@link Node} based on the Node's type.
     *
     * @param node a node to construct as a literal.
     * @return the literal representation of the node.
     */
    public static Literal fromNode(Node node) {
        ILiteral iLiteral = node.accept(new NodeVisitor<ILiteral>() {
            @Override
            public ILiteral arrayNode(ArrayNode arrayNode) {
                return new Tuple(arrayNode.getElements().stream()
                        .map(el -> new Literal(el.accept(this), el))
                        .collect(Collectors.toList()));
            }

            @Override
            public ILiteral booleanNode(BooleanNode booleanNode) {
                return new Bool(booleanNode);
            }

            @Override
            public ILiteral nullNode(NullNode nullNode) {
                throw new RuntimeException("null node not supported");
            }

            @Override
            public ILiteral numberNode(NumberNode numberNode) {
                return new Integer(numberNode);
            }

            @Override
            public ILiteral objectNode(ObjectNode objectNode) {
                Map<Identifier, Literal> obj = new HashMap<>();
                objectNode.getMembers().forEach((k, v) -> {
                    obj.put(Identifier.of(k), new Literal(v.accept(this), v));
                });
                return new Record(obj);
            }

            @Override
            public ILiteral stringNode(StringNode stringNode) {
                return new String(new Template(stringNode));
            }
        });
        return new Literal(iLiteral, node.getSourceLocation());
    }

    /**
     * Attempts to convert the literal to a {@link java.lang.String}. Otherwise throws an exception.
     *
     * @return the literal as a string.
     */
    public java.lang.String expectLiteralString() {
        if (source instanceof String) {
            final String s = (String) source;

            return s.value.expectLiteral();
        } else {
            throw new RuleError(new SourceException("Expected a literal string, got " + source, this));
        }
    }

    /**
     * Attempts to convert the literal to a {@link Boolean} if possible. Otherwise, returns an empty optional.
     *
     * @return an optional boolean.
     */
    public Optional<Boolean> asBool() {
        return source.asBool();
    }

    /**
     * Attempts to convert the literal to a {@link Template} if possible. Otherwise, returns an empty optional.
     *
     * @return an optional boolean.
     */
    public Optional<Template> asString() {
        return source.asString();
    }

    /**
     * Attempts to convert the literal to a map of {@link Identifier} to {@link Literal}.
     * Otherwise, returns an empty optional.
     *
     * @return an optional map.
     */
    public Optional<Map<Identifier, Literal>> asRecord() {
        return source.asRecord();
    }

    /**
     * Attempts to convert the literal to a list of {@link Literal} values.
     * Otherwise, returns an empty optional.
     *
     * @return the optional list.
     */
    public Optional<List<Literal>> asTuple() {
        return source.asTuple();
    }

    /**
     * Attempts to convert the literal to an {@link Integer}.
     * Otherwise, returns an empty optional.
     *
     * @return the optional integer.
     */
    public Optional<java.lang.Integer> asInteger() {
        return source.asInteger();
    }

    /**
     * Invokes the provided {@link Vistor}.
     *
     * @param visitor the visitor.
     * @param <T> the visitor return type.
     * @return the return value of the vistor.
     */
    public <T> T accept(Vistor<T> visitor) {
        return this.source.accept(visitor);
    }

    private Type nodeToType(ILiteral value, Scope<Type> scope) {
        return value.accept(new Vistor<Type>() {
            @Override
            public Type visitBool(boolean b) {
                return Type.bool();
            }

            @Override
            public Type visitString(Template value) {
                return value.typeCheck(scope);
            }

            @Override
            public Type visitRecord(Map<Identifier, Literal> members) {
                Map<Identifier, Type> tpe = new HashMap<>();
                ((Record) value).members.forEach((k, v) -> {
                    tpe.put(k, v.typeCheck(scope));
                });
                return new Type.Record(tpe);
            }

            @Override
            public Type visitTuple(List<Literal> members) {
                List<Type> tuples = new ArrayList<>();
                for (Literal el : ((Tuple) value).members) {
                    tuples.add(el.typeCheck(scope));
                }
                return new Type.Tuple(tuples);
            }

            @Override
            public Type visitInteger(int value) {
                return Type.integer();
            }
        });
    }

    /**
     * Returns the string representation of the literal.
     *
     * @return the string representation.
     */
    public java.lang.String toString() {
        return source.toString();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return SourceLocation.none();
    } // TODO(rcoh)

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitLiteral(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Literal literal = (Literal) o;
        return source.equals(literal.source);
    }

    @Override
    public Type typeCheckLocal(Scope<Type> scope) {
        return nodeToType(source, scope);
    }

    /**
     * @param evaluator the rule-set evaluator.
     * @return the resulting value.
     */
    public Value evaluate(RuleEvaluator evaluator) {
        return source.accept(new Vistor<Value>() {
            @Override
            public Value visitBool(boolean b) {
                return Value.bool(b);
            }

            @Override
            public Value visitString(Template value) {
                return Value.string(value.accept(new TemplateVisitor<java.lang.String>() {
                    @Override
                    public java.lang.String visitStaticTemplate(java.lang.String value) {
                        return value;
                    }

                    @Override
                    public java.lang.String visitSingleDynamicTemplate(Expression value) {
                        return value.accept(evaluator).expectString();
                    }

                    @Override
                    public java.lang.String visitStaticElement(java.lang.String value) {
                        return value;
                    }

                    @Override
                    public java.lang.String visitDynamicElement(Expression value) {
                        return value.accept(evaluator).expectString();
                    }

                    @Override
                    public java.lang.String startMultipartTemplate() {
                        return "";
                    }

                    @Override
                    public java.lang.String finishMultipartTemplate() {
                        return "";
                    }
                }).collect(Collectors.joining()));
            }

            @Override
            public Value visitRecord(Map<Identifier, Literal> members) {
                Map<Identifier, Value> tpe = new HashMap<>();
                members.forEach((k, v) -> {
                    tpe.put(k, v.accept(evaluator));
                });
                return Value.record(tpe);
            }

            @Override
            public Value visitTuple(List<Literal> members) {
                List<Value> tuples = new ArrayList<>();
                for (Literal el : ((Tuple) source).members) {
                    tuples.add(el.accept(evaluator));
                }
                return Value.array(tuples);
            }

            @Override
            public Value visitInteger(int value) {
                return Value.integer(value);
            }
        });
    }

    @Override
    public Node toNode() {
        return source.accept(new Vistor<Node>() {
            @Override
            public Node visitBool(boolean b) {
                return BooleanNode.from(b);
            }

            @Override
            public Node visitString(Template value) {
                return value.toNode();
            }

            @Override
            public Node visitRecord(Map<Identifier, Literal> members) {
                ObjectNode.Builder builder = ObjectNode.builder();
                members.forEach((k, v) -> builder.withMember(k.toString(), v.accept(this)));
                return builder.build();
            }

            @Override
            public Node visitTuple(List<Literal> members) {
                return ArrayNode.fromNodes(members.stream()
                        .map(member -> member.accept(this))
                        .collect(Collectors.toList()));
            }

            @Override
            public Node visitInteger(int value) {
                return Node.from(value);
            }
        });
    }

    // TODO: extract `LiteralVisitor` to follow Smithy pattern
    public interface Vistor<T> {
        T visitBool(boolean b);

        T visitString(Template value);

        T visitRecord(Map<Identifier, Literal> members);

        T visitTuple(List<Literal> members);

        T visitInteger(int value);
    }

    private interface ILiteral {
        <T> T accept(Vistor<T> visitor);

        default Optional<Boolean> asBool() {
            return Optional.empty();
        }

        default Optional<Template> asString() {
            return Optional.empty();
        }

        default Optional<Map<Identifier, Literal>> asRecord() {
            return Optional.empty();
        }

        default Optional<List<Literal>> asTuple() {
            return Optional.empty();
        }

        default Optional<java.lang.Integer> asInteger() {
            return Optional.empty();
        }
    }

    static final class Integer implements ILiteral {
        private final NumberNode value;

        Integer(NumberNode value) {
            if (!value.isNaturalNumber()) {
                throw new RuntimeException("only integers >= 0 are supported");
            }
            this.value = value;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitInteger(value.getValue().intValue());
        }

        @Override
        public Optional<java.lang.Integer> asInteger() {
            return Optional.of(this.value.getValue().intValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Integer anInteger = (Integer) o;
            return value.equals(anInteger.value);
        }

        @Override
        public java.lang.String toString() {
            return java.lang.Integer.toString(value.getValue().intValue());
        }
    }

    static final class Tuple implements ILiteral {
        private final List<Literal> members;

        Tuple(List<Literal> members) {
            this.members = members;
        }

        public List<Literal> members() {
            return members;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitTuple(members);
        }

        @Override
        public Optional<List<Literal>> asTuple() {
            return Optional.of(members);
        }

        @Override
        public int hashCode() {
            return Objects.hash(members);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Tuple that = (Tuple) obj;
            return Objects.equals(this.members, that.members);
        }

        @Override
        public java.lang.String toString() {
            return members.stream().map(Literal::toString).collect(Collectors.joining(", ", "[", "]"));
        }

    }

    static final class Record implements ILiteral {
        private final Map<Identifier, Literal> members;

        Record(Map<Identifier, Literal> members) {
            this.members = members;
        }

        public Map<Identifier, Literal> members() {
            return members;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitRecord(members);
        }

        @Override
        public Optional<Map<Identifier, Literal>> asRecord() {
            return Optional.of(members);
        }

        @Override
        public int hashCode() {
            return Objects.hash(members);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Record that = (Record) obj;
            return Objects.equals(this.members, that.members);
        }

        @Override
        public java.lang.String toString() {
            return members.toString();
        }

    }

    static final class Bool implements ILiteral {
        private final BooleanNode value;

        Bool(BooleanNode value) {
            this.value = value;
        }

        public BooleanNode value() {
            return value;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitBool(value.getValue());
        }

        @Override
        public Optional<Boolean> asBool() {
            return Optional.of(value.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            Bool that = (Bool) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public java.lang.String toString() {
            return value.toString();
        }

    }

    static final class String implements ILiteral {
        private final Template value;

        String(Template value) {
            this.value = value;
        }

        public Template value() {
            return value;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitString(value);
        }

        @Override
        public Optional<Template> asString() {
            return Optional.of(value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj == null || obj.getClass() != this.getClass()) {
                return false;
            }
            String that = (String) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public java.lang.String toString() {
            return value.toString();
        }

    }
}
