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

package software.amazon.smithy.rulesengine.language.syntax.functions;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.RulesComponentBuilder;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.error.InvalidRulesException;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.type.ArrayType;
import software.amazon.smithy.rulesengine.language.eval.type.RecordType;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set expression for indexing a record/object or array.
 */
@SmithyUnstableApi
public final class GetAttr extends Expression {
    public static final String ID = "getAttr";
    private final Expression target;
    private String unparsedPath;
    private final List<Part> path;

    private GetAttr(Builder builder) {
        super(builder.getSourceLocation());
        this.target = builder.target;
        this.unparsedPath = builder.path;
        this.path = parse(builder.path, builder.getSourceLocation());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Parses the path argument to getAttr.
     *
     * @param path           path argument in the form `a.b[5]`
     * @param sourceLocation Source location for tracking errors
     * @return List of parsed `Part`s
     */
    private static List<Part> parse(String path, FromSourceLocation sourceLocation) {
        String[] components = path.split("\\.");
        List<Part> result = new ArrayList<>();
        for (String component : components) {
            if (component.contains("[")) {
                int slicePartIndex = component.indexOf("[");
                String slicePart = component.substring(slicePartIndex);
                if (!slicePart.endsWith("]")) {
                    throw new InvalidRulesException("Invalid path component: %s. Must end with `]`",
                            sourceLocation);
                }
                try {
                    String number = slicePart.substring(1, slicePart.length() - 1);
                    int slice = Integer.parseInt(number);
                    if (slice < 0) {
                        throw new InvalidRulesException("Invalid path component: slice index must be >= 0",
                                sourceLocation);
                    }
                    result.add(Part.Key.of(component.substring(0, slicePartIndex)));
                    result.add(new Part.Index(slice));
                } catch (NumberFormatException ex) {
                    throw new InvalidRulesException(String.format("%s could not be parsed as a number", slicePart),
                            sourceLocation);
                }
            } else {
                result.add(Part.Key.of(component));
            }
        }
        if (result.isEmpty()) {
            throw new InvalidRulesException("Invalid argument to GetAttr: path may not be empty", sourceLocation);
        }
        return result;
    }

    public Value evaluate(Value target) {
        Value root = target;
        List<Part> path = getPath();
        for (Part part : path) {
            root = part.eval(root);
        }
        return root;
    }

    public Expression getTarget() {
        return target;
    }

    public List<Part> getPath() {
        return path;
    }

    @Override
    public String toString() {
        return target + "#" + unparsedPath;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitGetAttr(this);
    }

    @Override
    public Type typeCheckLocal(Scope<Type> scope) {
        Expression target = getTarget();
        List<Part> path = new ArrayList<>(getPath());
        Type base = target.typeCheck(scope);

        for (Part part : path) {
            Type finalBase = base;
            base = context(String.format("while resolving %s in %s", part, base), this,
                    () -> part.typeCheck(finalBase));
        }
        return base;
    }

    @Override
    public String getTemplate() {
        String target = ((Reference) this.getTarget()).getName().asString();
        return "{" + target + "#" + unparsedPath + "}";
    }

    @Override
    public Node toNode() {
        // Synthesize an fn-node:
        return ObjectNode.builder()
                .withMember("fn", GetAttr.ID)
                .withMember("argv", ArrayNode.arrayNode(target.toNode(), StringNode.from(unparsedPath))).build();
    }

    /**
     * Convert this function into a condition.
     *
     * @return the function as a condition.
     */
    public Condition condition() {
        return new Condition.Builder().fn(this).build();
    }

    /**
     * Converts this function into a condition which stores the output in the named result.
     *
     * @param result the name of the result parameter.
     * @return the function as a condition.
     */
    public Condition condition(String result) {
        return new Condition.Builder().fn(this).result(result).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GetAttr getAttr = (GetAttr) o;
        return target.equals(getAttr.target) && path.equals(getAttr.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, path);
    }

    public interface Part {
        Type typeCheck(Type container) throws InnerParseError;

        Value eval(Value container);

        final class Key implements Part {
            private final Identifier key;

            public Key(Identifier key) {
                this.key = key;
            }

            public static Key of(String key) {
                return new Key(Identifier.of(key));
            }

            public Type typeCheck(Type container) throws InnerParseError {
                RecordType record = container.expectRecordType(String.format("cannot index into %s, expected object",
                        container));
                return record
                        .get(key)
                        .orElseThrow(() -> new InnerParseError(String.format("%s does not contain field %s",
                                container, key)));
            }

            @Override
            public Value eval(Value container) {
                return container.expectRecordValue().get(key);
            }

            public Identifier key() {
                return key;
            }

            @Override
            public int hashCode() {
                return Objects.hash(key);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                Key that = (Key) obj;
                return Objects.equals(this.key, that.key);
            }

            @Override
            public String toString() {
                return key.asString();
            }

        }

        final class Index implements Part {
            private final int index;

            public Index(int index) {
                this.index = index;
            }

            @Override
            public Type typeCheck(Type container) throws InnerParseError {
                ArrayType arr = container.expectArrayType();
                return Type.optionalType(arr.getMember());
            }

            @Override
            public Value eval(Value container) {
                return container.expectArrayValue().get(index);
            }

            public int index() {
                return index;
            }

            @Override
            public int hashCode() {
                return Objects.hash(index);
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == this) {
                    return true;
                }
                if (obj == null || obj.getClass() != this.getClass()) {
                    return false;
                }
                Index that = (Index) obj;
                return this.index == that.index;
            }

            @Override
            public String toString() {
                return String.format("[%s]", index);
            }

        }
    }

    public static final class Builder extends RulesComponentBuilder<Builder, GetAttr> {
        Expression target;
        String path;

        private Builder() {
            super(SourceLocation.none());
        }

        public Builder target(Expression target) {
            this.target = target;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        @Override
        public GetAttr build() {
            return new GetAttr(this);
        }
    }
}
