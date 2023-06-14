/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * A function which is constructed from a {@link FunctionDefinition}.
 */
@SmithyUnstableApi
public abstract class LibraryFunction extends Expression {
    protected final FunctionDefinition definition;
    protected final FunctionNode functionNode;

    public LibraryFunction(FunctionDefinition definition, FunctionNode functionNode) {
        super(functionNode.getSourceLocation());
        this.definition = definition;
        this.functionNode = functionNode;
    }

    /**
     * Returns the name of this function, eg. {@code isSet}, {@code parseUrl}
     *
     * @return The name
     */
    public String getName() {
        return functionNode.getName();
    }

    /**
     * @return The arguments to this function
     */
    public List<Expression> getArguments() {
        return functionNode.getArguments();
    }

    protected Expression expectOneArgument() {
        List<Expression> argv = functionNode.getArguments();
        if (argv.size() == 1) {
            return argv.get(0);
        }
        throw new RuleError(new SourceException("expected 1 argument but found " + argv.size(), functionNode));
    }

    @Override
    public SourceLocation getSourceLocation() {
        return functionNode.getSourceLocation();
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        RuleError.context(String.format("while typechecking the invocation of %s", definition.getId()), this, () -> {
            try {
                checkTypeSignature(definition.getArguments(), functionNode.getArguments(), scope);
            } catch (InnerParseError e) {
                throw new RuntimeException(e.getMessage());
            }
        });
        return definition.getReturnType();
    }

    private void checkTypeSignature(List<Type> expectedArgs, List<Expression> actualArguments, Scope<Type> scope)
            throws InnerParseError {
        if (expectedArgs.size() != actualArguments.size()) {
            throw new InnerParseError(
                    String.format(
                            "Expected %s arguments but found %s",
                            expectedArgs.size(),
                            actualArguments)
            );
        }
        for (int i = 0; i < expectedArgs.size(); i++) {
            Type expected = expectedArgs.get(i);
            Type actual = actualArguments.get(i).typeCheck(scope);
            if (!expected.isA(actual)) {
                Type optAny = Type.optionalType(Type.anyType());
                String hint = "";
                if (actual.isA(optAny) && !expected.isA(optAny)
                            && actual.expectOptionalType().inner().equals(expected)) {
                    hint = String.format(
                            "hint: use `assign` in a condition or `isSet(%s)` to prove that this value is non-null",
                            actualArguments.get(i));
                    hint = StringUtils.indent(hint, 2);
                }
                throw new InnerParseError(
                        String.format(
                                "Unexpected type in the %s argument: Expected %s but found %s%n%s",
                                ordinal(i + 1), expected, actual, hint)
                );
            }
        }
    }

    private static String ordinal(int arg) {
        switch (arg) {
            case 1:
                return "first";
            case 2:
                return "second";
            case 3:
                return "third";
            case 4:
                return "fourth";
            case 5:
                return "fifth";
            default:
                return String.format("%s", arg);
        }
    }

    @Override
    public Node toNode() {
        return functionNode.toNode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof LibraryFunction)) {
            return false;
        }

        return ((LibraryFunction) obj).functionNode.equals(this.functionNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionNode);
    }

    @Override
    public String toString() {
        List<String> arguments = new ArrayList<>();
        for (Expression expression : getArguments()) {
            arguments.add(expression.toString());
        }
        return getName() + "(" + String.join(", ", arguments) + ")";
    }
}
