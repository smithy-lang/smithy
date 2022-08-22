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

package software.amazon.smithy.rulesengine.reterminus.lang.expr;

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
import software.amazon.smithy.rulesengine.reterminus.SourceAwareBuilder;
import software.amazon.smithy.rulesengine.reterminus.error.RuleError;
import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Value;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.visit.ExprVisitor;

public final class Literal extends Expr {

    private final Lit source;

    private Literal(Lit source, FromSourceLocation sourceLocation) {
        super(sourceLocation.getSourceLocation());
        this.source = source;
    }

    public static Literal tuple(List<Literal> authSchemes) {
        return new Literal(new Tuple(authSchemes), SourceAwareBuilder.javaLocation());
    }

    public static Literal record(Map<Identifier, Literal> record) {
        return new Literal(new Obj(record), SourceAwareBuilder.javaLocation());
    }

    public static Literal str(Template value) {
        return new Literal(new Str(value), SourceAwareBuilder.javaLocation());
    }

    public static Literal integer(int value) {
        return new Literal(new Int(Node.from(value)), SourceAwareBuilder.javaLocation());
    }

    public static Literal bool(boolean value) {
        return new Literal(new Bool(Node.from(value)), SourceAwareBuilder.javaLocation());
    }

    public static Literal fromNode(Node node) {
        Lit lit = node.accept(new NodeVisitor<Lit>() {
            @Override
            public Lit arrayNode(ArrayNode arrayNode) {
                return new Tuple(arrayNode.getElements().stream()
                        .map(el -> new Literal(el.accept(this), el))
                        .collect(Collectors.toList()));
            }

            @Override
            public Lit booleanNode(BooleanNode booleanNode) {
                return new Bool(booleanNode);
            }

            @Override
            public Lit nullNode(NullNode nullNode) {
                throw new RuntimeException("null node not supported");
            }

            @Override
            public Lit numberNode(NumberNode numberNode) {
                return new Int(numberNode);
            }

            @Override
            public Lit objectNode(ObjectNode objectNode) {
                Map<Identifier, Literal> obj = new HashMap<>();
                objectNode.getMembers().forEach((k, v) -> {
                    obj.put(Identifier.of(k), new Literal(v.accept(this), v));
                });
                return new Obj(obj);
            }

            @Override
            public Lit stringNode(StringNode stringNode) {
                return new Str(new Template(stringNode));
            }
        });
        return new Literal(lit, node.getSourceLocation());
    }

    public String expectLiteralString() {
        if (source instanceof Str) {
            final Str s = (Str) source;

            return s.value.expectLiteral();
        } else {
            throw new RuleError(new SourceException("Expected a literal string, got " + source, this));
        }
    }

    public Optional<Boolean> asBool() {
        return source.asBool();
    }

    public Optional<Template> asString() {
        return source.asString();
    }

    public Optional<Map<Identifier, Literal>> asObject() {
        return source.asObject();
    }

    public Optional<List<Literal>> asTuple() {
        return source.asTuple();
    }

    public Optional<Integer> asInt() {
        return source.asInt();
    }

    public <T> T accept(Vistor<T> visitor) {
        return this.source.accept(visitor);
    }

    private Type nodeToType(Lit value, Scope<Type> scope) {
        return value.accept(new Vistor<Type>() {
            @Override
            public Type visitBool(boolean b) {
                return Type.bool();
            }

            @Override
            public Type visitStr(Template value) {
                return value.typecheck(scope);
            }

            @Override
            public Type visitObject(Map<Identifier, Literal> members) {
                Map<Identifier, Type> tpe = new HashMap<>();
                ((Obj) value).members.forEach((k, v) -> {
                    tpe.put(k, v.typecheck(scope));
                });
                return new Type.Record(tpe);
            }

            @Override
            public Type visitTuple(List<Literal> members) {
                List<Type> tuples = new ArrayList<>();
                for (Literal el : ((Tuple) value).members) {
                    tuples.add(el.typecheck(scope));
                }
                return new Type.Tuple(tuples);
            }

            @Override
            public Type visitInt(int value) {
                return Type.integer();
            }
        });
    }

    public String toString() {
        return source.toString();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return SourceLocation.none();
    } // TODO(rcoh)

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
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
    public Type typecheckLocal(Scope<Type> scope) {
        return nodeToType(source, scope);
    }

    @Override
    public Value eval(Scope<Value> scope) {
        return source.accept(new Vistor<Value>() {
            @Override
            public Value visitBool(boolean b) {
                return Value.bool(b);
            }

            @Override
            public Value visitStr(Template value) {
                return value.eval(scope);
            }

            @Override
            public Value visitObject(Map<Identifier, Literal> members) {
                Map<Identifier, Value> tpe = new HashMap<>();
                members.forEach((k, v) -> {
                    tpe.put(k, v.eval(scope));
                });
                return Value.record(tpe);
            }

            @Override
            public Value visitTuple(List<Literal> members) {
                List<Value> tuples = new ArrayList<>();
                for (Literal el : ((Tuple) source).members) {
                    tuples.add(el.eval(scope));
                }
                return Value.array(tuples);
            }

            @Override
            public Value visitInt(int value) {
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
            public Node visitStr(Template value) {
                return value.toNode();
            }

            @Override
            public Node visitObject(Map<Identifier, Literal> members) {
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
            public Node visitInt(int value) {
                return Node.from(value);
            }
        });
    }

    // TODO: extract `LiteralVisitor` to follow Smithy pattern
    public interface Vistor<T> {
        T visitBool(boolean b);

        T visitStr(Template value);

        T visitObject(Map<Identifier, Literal> members);

        T visitTuple(List<Literal> members);

        T visitInt(int value);
    }

    private interface Lit {
        <T> T accept(Vistor<T> visitor);

        default Optional<Boolean> asBool() {
            return Optional.empty();
        }

        default Optional<Template> asString() {
            return Optional.empty();
        }

        default Optional<Map<Identifier, Literal>> asObject() {
            return Optional.empty();
        }

        default Optional<List<Literal>> asTuple() {
            return Optional.empty();
        }

        default Optional<Integer> asInt() {
            return Optional.empty();
        }
    }

    static final class Int implements Lit {
        private final NumberNode value;

        Int(NumberNode value) {
            if (!value.isNaturalNumber()) {
                throw new RuntimeException("only integers >= 0 are supported");
            }
            this.value = value;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitInt(value.getValue().intValue());
        }

        @Override
        public Optional<Integer> asInt() {
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
            Int anInt = (Int) o;
            return value.equals(anInt.value);
        }

        @Override
        public String toString() {
            return Integer.toString(value.getValue().intValue());
        }
    }

    static final class Tuple implements Lit {
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
        public String toString() {
            return members.stream().map(Literal::toString).collect(Collectors.joining(", ", "[", "]"));
        }

    }

    static final class Obj implements Lit {
        private final Map<Identifier, Literal> members;

        Obj(Map<Identifier, Literal> members) {
            this.members = members;
        }

        public Map<Identifier, Literal> members() {
            return members;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitObject(members);
        }

        @Override
        public Optional<Map<Identifier, Literal>> asObject() {
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
            Obj that = (Obj) obj;
            return Objects.equals(this.members, that.members);
        }

        @Override
        public String toString() {
            return members.toString();
        }

    }

    static final class Bool implements Lit {
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
        public String toString() {
            return value.toString();
        }

    }

    static final class Str implements Lit {
        private final Template value;

        Str(Template value) {
            this.value = value;
        }

        public Template value() {
            return value;
        }

        @Override
        public <T> T accept(Vistor<T> visitor) {
            return visitor.visitStr(value);
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
            Str that = (Str) obj;
            return Objects.equals(this.value, that.value);
        }

        @Override
        public String toString() {
            return value.toString();
        }

    }
}
