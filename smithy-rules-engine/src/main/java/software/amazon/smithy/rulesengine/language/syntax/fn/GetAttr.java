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

package software.amazon.smithy.rulesengine.language.syntax.fn;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.error.InvalidRulesException;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.expr.Ref;
import software.amazon.smithy.rulesengine.language.util.SourceLocationTrackingBuilder;
import software.amazon.smithy.rulesengine.language.visit.ExprVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class GetAttr extends Expr {
    public static final String ID = "getAttr";
    private final Expr target;
    private final List<Part> path;

    private GetAttr(Builder builder) {
        super(builder.getSourceLocation());
        this.target = builder.target;
        this.path = parse(builder.path, builder.getSourceLocation());
    }

    public static Builder builder(FromSourceLocation context) {
        return new Builder(context);
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

    public Value eval(Value target) {
        Value root = target;
        List<Part> path = getPath();
        for (Part part : path) {
            root = part.eval(root);
        }
        return root;
    }

    public Expr getTarget() {
        return target;
    }

    public List<Part> getPath() {
        return path;
    }

    @Override
    public String toString() {
        return target + "#" + unparse();
    }

    @Override
    public <R> R accept(ExprVisitor<R> visitor) {
        return visitor.visitGetAttr(this);
    }

    @Override
    public Type typecheckLocal(Scope<Type> scope) {
        Expr target = getTarget();
        List<Part> path = new ArrayList<>(getPath());
        Type base = target.typecheck(scope);

        for (Part part : path) {
            Type finalBase = base;
            base = ctx(String.format("while resolving %s in %s", part, base), this, () -> part.typecheck(finalBase));
        }
        return base;
    }

    @Override
    public String template() {
        String target = ((Ref) this.getTarget()).getName().asString();
        return "{" + target + "#" + unparse() + "}";
    }

    @Override
    public Node toNode() {
        // need to synthesize an fn-node:
        // { "fn": "getAttr", "argv": [<target>, path] }
        return ObjectNode.builder()
                .withMember("fn", GetAttr.ID)
                .withMember("argv", ArrayNode.arrayNode(target.toNode(), StringNode.from(unparse()))).build();
    }

    private String unparse() {
        StringBuilder path = new StringBuilder();
        boolean first = true;
        for (Part part : getPath()) {
            if (!first && part instanceof Part.Key) {
                path.append(".");
            }
            first = false;
            path.append(part);
        }
        return path.toString();
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
        Type typecheck(Type container) throws InnerParseError;

        Value eval(Value container);

        final class Key implements Part {
            private final Identifier key;

            public Key(Identifier key) {
                this.key = key;
            }

            public static Key of(String key) {
                return new Key(Identifier.of(key));
            }

            public Type typecheck(Type container) throws InnerParseError {
                Type.Record record = container.expectObject(String.format("cannot index into %s, expected object",
                        container));
                return record
                        .get(key)
                        .orElseThrow(() -> new InnerParseError(String.format("%s does not contain field %s",
                                container, key)));
            }

            @Override
            public Value eval(Value container) {
                return container.expectRecord().get(key);
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
            public Type typecheck(Type container) throws InnerParseError {
                Type.Array arr = container.expectArray();
                return Type.optional(arr.getMember());
            }

            @Override
            public Value eval(Value container) {
                return container.expectArray().get(index);
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

    public static class Builder extends SourceLocationTrackingBuilder<Builder, GetAttr> {
        Expr target;
        String path;

        public Builder(FromSourceLocation sourceLocation) {
            super(sourceLocation);
        }

        public Builder target(Expr target) {
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
