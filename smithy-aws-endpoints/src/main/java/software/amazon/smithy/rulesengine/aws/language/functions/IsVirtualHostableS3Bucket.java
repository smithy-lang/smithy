/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An AWS rule-set function for determining whether a given string can be promoted to an S3 virtual bucket host label.
 */
@SmithyUnstableApi
public final class IsVirtualHostableS3Bucket extends LibraryFunction {
    public static final String ID = "aws.isVirtualHostableS3Bucket";
    private static final Definition DEFINITION = new Definition();

    private IsVirtualHostableS3Bucket(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
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
     * Creates a {@link IsVirtualHostableS3Bucket} function from the given expressions.
     *
     * @param arg1 the value to check.
     * @param arg2 whether to allow subdomains.
     * @return The resulting {@link IsVirtualHostableS3Bucket} function.
     */
    public static IsVirtualHostableS3Bucket ofExpressions(ToExpression arg1, ToExpression arg2) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1, arg2));
    }


    /**
     * Creates a {@link IsVirtualHostableS3Bucket} function from the given expressions.
     *
     * @param arg1 the value to check.
     * @param arg2 whether to allow subdomains.
     * @return The resulting {@link IsVirtualHostableS3Bucket} function.
     */
    public static IsVirtualHostableS3Bucket ofExpressions(ToExpression arg1, boolean arg2) {
        return ofExpressions(arg1, Expression.of(arg2));
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link IsVirtualHostableS3Bucket} function.
     */
    public static final class Definition implements FunctionDefinition {
        private static final Pattern DOTS_ALLOWED = Pattern.compile("[a-z\\d][a-z\\d\\-.]{1,61}[a-z\\d]");
        private static final Pattern DOTS_DISALLOWED = Pattern.compile("[a-z\\d][a-z\\d\\-]{1,61}[a-z\\d]");
        private static final Pattern IP_ADDRESS = Pattern.compile("(\\d+\\.){3}\\d+");
        private static final Pattern DASH_DOT_SEPARATOR = Pattern.compile(".*[.-]{2}.*");

        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(Type.stringType(), Type.booleanType());
        }

        @Override
        public Type getReturnType() {
            return Type.booleanType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String hostLabel = arguments.get(0).expectStringValue().getValue();
            boolean allowDots = arguments.get(1).expectBooleanValue().getValue();
            if (allowDots) {
                return Value.booleanValue(
                        DOTS_ALLOWED.matcher(hostLabel).matches()
                        // Don't allow ip address
                        && !IP_ADDRESS.matcher(hostLabel).matches()
                        // Don't allow names like bucket-.name or bucket.-name
                        && !DASH_DOT_SEPARATOR.matcher(hostLabel).matches()
                );
            } else {
                return Value.booleanValue(DOTS_DISALLOWED.matcher(hostLabel).matches());
            }
        }

        @Override
        public IsVirtualHostableS3Bucket createFunction(FunctionNode functionNode) {
            return new IsVirtualHostableS3Bucket(functionNode);
        }
    }
}
