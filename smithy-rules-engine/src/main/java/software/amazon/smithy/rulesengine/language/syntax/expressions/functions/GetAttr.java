/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

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
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.ArrayType;
import software.amazon.smithy.rulesengine.language.evaluation.type.RecordType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set expression for indexing a record/object or array.
 */
@SmithyUnstableApi
public final class GetAttr extends LibraryFunction {
    public static final String ID = "getAttr";
    private static final Definition DEFINITION = new Definition();

    private final Expression target;
    private final String unparsedPath;
    private final List<Part> path;

    private GetAttr(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
        this.target = functionNode.getArguments().get(0);
        this.unparsedPath = functionNode.getArguments().get(1).toNode().expectStringNode().getValue();
        this.path = parse(unparsedPath, functionNode.getArguments().get(1));
    }

    /**
     * Gets the {@link FunctionDefinition} implementation.
     *
     * @return the function definition.
     */
    public static Definition getDefinition() {
        return DEFINITION;
    }

    /**
     * Creates a {@link GetAttr} function from the given expressions.
     *
     * @param arg1 the argument to extract from.
     * @param arg2 the path to extract.
     * @return The resulting {@link GetAttr} function.
     */
    public static GetAttr ofExpressions(ToExpression arg1, ToExpression arg2) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1, arg2));
    }

    /**
     * Creates a {@link GetAttr} function from the given expressions.
     *
     * @param arg1 the argument to extract from.
     * @param arg2 the path to extract.
     * @return The resulting {@link GetAttr} function.
     */
    public static GetAttr ofExpressions(ToExpression arg1, String arg2) {
        return ofExpressions(arg1, Expression.of(arg2));
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

    /**
     * Gets the value at the defined path out of the target value.
     *
     * @param target the target value to retrieve a value out of.
     * @return the retrieve value.
     */
    public Value evaluate(Value target) {
        Value root = target;
        for (Part part : path) {
            root = part.eval(root);
        }
        return root;
    }

    /**
     * Gets the expression to retrieve an attribute of.
     *
     * @return the expression targeted by this function.
     */
    public Expression getTarget() {
        return target;
    }

    /**
     * Gets the list of {@link Part}s that make up the path to the requested attribute.
     *
     * @return the list of path parts to the requested attribute.
     */
    public List<Part> getPath() {
        return path;
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitGetAttr(this);
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        Type base = target.typeCheck(scope);
        for (Part part : path) {
            Type baseType = base;
            base = context(String.format("while resolving %s in %s", part, base), this, () -> part.typeCheck(baseType));
        }
        return base;
    }

    @Override
    public String template() {
        String targetString = ((Reference) target).getName().toString();
        return "{" + targetString + "#" + unparsedPath + "}";
    }

    @Override
    public Node toNode() {
        // Synthesize an fn-node:
        return ObjectNode.builder()
                .withMember("fn", GetAttr.ID)
                .withMember("argv", ArrayNode.arrayNode(target.toNode(), StringNode.from(unparsedPath))).build();
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

    @Override
    public String toString() {
        return target + "#" + unparsedPath;
    }

    /**
     * A {@link FunctionDefinition} for the {@link GetAttr} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            // First argument is array or record, so we need to use any here and typecheck it elsewhere.
            return ListUtils.of(Type.anyType(), Type.stringType());
        }

        @Override
        public Type getReturnType() {
            return Type.anyType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            // Specialized in the ExpressionVisitor, so this doesn't need an implementation.
            return null;
        }

        @Override
        public GetAttr createFunction(FunctionNode functionNode) {
            return new GetAttr(functionNode);
        }
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
                return record.get(key).orElseThrow(() ->
                        new InnerParseError(String.format("%s does not contain field %s", container, key)));
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
                return key.toString();
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
}
