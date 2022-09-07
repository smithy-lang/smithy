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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.SourceAwareBuilder;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expr.Ref;
import software.amazon.smithy.rulesengine.language.visit.FnVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class GetAttr extends Fn {
    public static final String ID = "getAttr";

    public GetAttr(FnNode node) {
        super(node);
    }

    private static GetAttr fromBuilder(Builder builder) {
        return new GetAttr(FnNode
                .builder(builder.getSourceLocation())
                .fn(StringNode.from("getAttr"))
                .argv(
                        Arrays.asList(
                                builder.target,
                                Literal.of(String.join(".", builder.path))))
                .build());
    }

    public static Builder builder(FromSourceLocation context) {
        return new Builder(context);
    }

    private static List<Part> parse(String path) throws InnerParseError {
        String[] components = path.split("\\.");
        List<Part> result = new ArrayList<>();
        for (String component : components) {
            if (component.contains("[")) {
                int slicePartIndex = component.indexOf("[");
                String slicePart = component.substring(slicePartIndex);
                if (!slicePart.endsWith("]")) {
                    throw new InnerParseError("Invalid path component: %s. Must end with `]`");
                }
                try {
                    String number = slicePart.substring(1, slicePart.length() - 1);
                    int slice = Integer.parseInt(number);
                    if (slice < 0) {
                        throw new InnerParseError("Invalid path component: slice index must be >= 0");
                    }
                    result.add(Part.Key.of(component.substring(0, slicePartIndex)));
                    result.add(new Part.Index(slice));
                } catch (NumberFormatException ex) {
                    throw new InnerParseError(String.format("%s could not be parsed as a number", slicePart));
                }
            } else {
                result.add(Part.Key.of(component));
            }
        }
        if (result.isEmpty()) {
            throw new InnerParseError("Invalid argument to GetAttr: path may not be empty");
        }
        return result;
    }

    @Override
    public Value eval(Scope<Value> scope) {
        Value root = target().eval(scope);
        List<Part> path = null;
        try {
            path = path();
        } catch (InnerParseError e) {
            throw new RuntimeException(e);
        }
        for (Part part : path) {
            root = part.eval(root);
        }
        return root;
    }

    public Expr target() {
        return expectTwoArgs().left;
    }

    public List<Part> path() throws InnerParseError {
        Expr right = expectTwoArgs().right;
        if (right instanceof Literal) {
            Literal path = (Literal) right;
            return parse(path.expectLiteralString());
        } else {
            throw new SourceException(String.format("second argument must be a string literal (%s, %s)",
                    right.getClass(), right), right);
        }
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitGetAttr(this);
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(target());
        try {
            for (Part part : path()) {
                out.append(".");
                out.append(part);
            }
        } catch (InnerParseError e) {
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    @Override
    public Type typecheckLocal(Scope<Type> scope) throws InnerParseError {
        Expr target = target();
        List<Part> path = new ArrayList<>(path());
        Type base = target.typecheck(scope);

        for (Part part : path) {
            Type finalBase = base;
            base = ctx(String.format("while resolving %s in %s", part, base), this, () -> part.typecheck(finalBase));
        }
        return base;
    }

    @Override
    public String template() {
        String target = ((Ref) this.target()).getName().asString();
        StringBuilder pathPart = new StringBuilder();

        List<Part> partList = null;
        try {
            partList = path();
        } catch (InnerParseError e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < partList.size(); i++) {
            if (i != 0) {
                if (partList.get(i) instanceof Part.Key) {
                    pathPart.append(".");
                }
            }
            pathPart.append(partList.get(i).toString());
        }
        return "{" + target + "#" + pathPart + "}";
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

    public static class Builder extends SourceAwareBuilder<Builder, GetAttr> {
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
            return GetAttr.fromBuilder(this);
        }
    }
}
