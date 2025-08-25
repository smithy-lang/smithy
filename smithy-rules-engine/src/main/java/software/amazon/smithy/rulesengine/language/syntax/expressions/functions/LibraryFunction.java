/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.error.InnerParseError;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
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

    @Override
    protected Set<String> calculateReferences() {
        Set<String> references = new LinkedHashSet<>();
        for (Expression arg : getArguments()) {
            references.addAll(arg.getReferences());
        }
        return references;
    }

    /**
     * Get the function definition.
     *
     * @return function definition.
     */
    public FunctionDefinition getFunctionDefinition() {
        return definition;
    }

    /**
     * @return The arguments to this function
     */
    public List<Expression> getArguments() {
        return functionNode.getArguments();
    }

    /**
     * Returns a canonical form of this function.
     *
     * <p>Default implementation returns this. Override for functions that need canonicalization.
     *
     * @return the canonical form of this function
     */
    public LibraryFunction canonicalize() {
        return this;
    }

    protected Expression expectOneArgument() {
        List<Expression> argv = functionNode.getArguments();
        if (argv.size() == 1) {
            return argv.get(0);
        }
        throw new RuleError(new SourceException("expected 1 argument but found " + argv.size(), functionNode));
    }

    @Override
    public Condition.Builder toConditionBuilder() {
        return Condition.builder().fn(this);
    }

    @Override
    public SourceLocation getSourceLocation() {
        return functionNode.getSourceLocation();
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        RuleError.context(String.format("while typechecking the invocation of %s", definition.getId()), this, () -> {
            try {
                checkTypeSignature(scope);
            } catch (InnerParseError e) {
                throw new RuntimeException(e.getMessage());
            }
        });
        return definition.getReturnType();
    }

    private void checkTypeSignature(Scope<Type> scope) throws InnerParseError {
        List<Type> expectedArgs = definition.getArguments();
        Optional<Type> variadicType = definition.getVariadicArguments();
        List<Expression> actualArguments = functionNode.getArguments();

        if (variadicType.isPresent()) {
            // check we have at least the fixed arguments
            if (actualArguments.size() < expectedArgs.size()) {
                throw new InnerParseError(String.format("Expected at least %s arguments but found %s",
                        expectedArgs.size(),
                        actualArguments.size()));
            }
            // check fixed arguments
            for (int i = 0; i < expectedArgs.size(); i++) {
                checkArgument(i, expectedArgs.get(i), actualArguments.get(i), scope);
            }
            // check variadic arguments
            Type varType = variadicType.get();
            for (int i = expectedArgs.size(); i < actualArguments.size(); i++) {
                checkArgument(i, varType, actualArguments.get(i), scope);
            }
        } else {
            // Non-variadic, so exact count required
            if (expectedArgs.size() != actualArguments.size()) {
                throw new InnerParseError(String.format("Expected %s arguments but found %s",
                        expectedArgs.size(),
                        actualArguments.size()));
            }
            // check all positional arguments
            for (int i = 0; i < expectedArgs.size(); i++) {
                checkArgument(i, expectedArgs.get(i), actualArguments.get(i), scope);
            }
        }
    }

    private void checkArgument(int index, Type expected, Expression actual, Scope<Type> scope)
            throws InnerParseError {
        Type actualType = actual.typeCheck(scope);
        if (expected.isA(actualType)) {
            return;
        }

        Type optAny = Type.optionalType(Type.anyType());
        String hint = "";
        if (actualType.isA(optAny)
                && !expected.isA(optAny)
                && actualType.expectOptionalType().inner().equals(expected)) {
            hint = String.format(
                    "hint: use `assign` in a condition or `isSet(%s)` to prove that this value is non-null",
                    actual);
            hint = StringUtils.indent(hint, 2);
        }

        throw new InnerParseError(String.format("Unexpected type in the %s argument: Expected %s but found %s%n%s",
                ordinal(index + 1),
                expected,
                actualType,
                hint));
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

    /**
     * Determines if two arguments should be swapped for canonical ordering.
     * Used by commutative functions to ensure consistent argument order.
     *
     * @param arg0 the first argument
     * @param arg1 the second argument
     * @return true if arguments should be swapped
     */
    protected static boolean shouldSwapArgs(Expression arg0, Expression arg1) {
        boolean arg0IsRef = isReference(arg0);
        boolean arg1IsRef = isReference(arg1);

        // Always put References before literals to make things consistent
        if (arg0IsRef != arg1IsRef) {
            return !arg0IsRef; // Swap if arg0 is literal and arg1 is reference
        }

        // Both same type, use string comparison for deterministic order
        return arg0.toString().compareTo(arg1.toString()) > 0;
    }

    /**
     * Strips single-variable template wrappers if present.
     * Converts "{varName}" to just varName reference.
     *
     * @param expr the expression to strip
     * @return the stripped expression or original if not applicable
     */
    static Expression stripSingleVariableTemplate(Expression expr) {
        if (!(expr instanceof StringLiteral)) {
            return expr;
        }

        StringLiteral stringLit = (StringLiteral) expr;
        List<Template.Part> parts = stringLit.value().getParts();
        if (parts.size() == 1 && parts.get(0) instanceof Template.Dynamic) {
            return ((Template.Dynamic) parts.get(0)).toExpression();
        }

        return expr;
    }

    private static boolean isReference(Expression arg) {
        if (arg instanceof Reference) {
            return true;
        } else if (arg instanceof StringLiteral) {
            StringLiteral s = (StringLiteral) arg;
            return !s.value().isStatic();
        }
        return false;
    }
}
