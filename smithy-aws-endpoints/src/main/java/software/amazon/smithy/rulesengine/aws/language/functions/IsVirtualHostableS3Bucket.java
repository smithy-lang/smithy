/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
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
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public int getCost() {
            return 80;
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
            return Value.booleanValue(isVirtualHostableBucket(hostLabel, allowDots));
        }

        @Override
        public IsVirtualHostableS3Bucket createFunction(FunctionNode functionNode) {
            return new IsVirtualHostableS3Bucket(functionNode);
        }
    }

    /**
     * Checks if the given hostLabel string is a virtual hostable bucket name.
     *
     * @param hostLabel Host label to check.
     * @param allowDots Whether to allow dots in the host label.
     * @return true if it is compatible.
     */
    public static boolean isVirtualHostableBucket(String hostLabel, boolean allowDots) {
        // Bucket names must be between 3 (min) and 63 (max) characters long.
        int bucketLength = hostLabel == null ? 0 : hostLabel.length();
        if (bucketLength < 3 || bucketLength > 63) {
            return false;
        }

        // Bucket names must begin and end with a letter or number.
        if (!isAlphanumeric(hostLabel.charAt(0)) || !isAlphanumeric(hostLabel.charAt(bucketLength - 1))) {
            return false;
        }

        // Bucket names can consist only of lowercase letters, numbers, periods (.), and hyphens (-).
        if (!allowDots) {
            for (int i = 1; i < bucketLength - 1; i++) { // already validated 0 and N - 1.
                if (!isValidBucketSegmentChar(hostLabel.charAt(i))) {
                    return false;
                }
            }
            return true;
        }

        // Check for consecutive dots or hyphens
        char last = hostLabel.charAt(0);
        for (int i = 1; i < bucketLength; i++) {
            char c = hostLabel.charAt(i);
            // Don't allow "bucket-.foo" or "bucket.-foo"
            if (c == '.') {
                if (last == '.' || last == '-') {
                    return false;
                }
            } else if (c == '-') {
                if (last == '.') {
                    return false;
                }
            } else if (!isAlphanumeric(c)) {
                return false;
            }
            last = c;
        }

        // Bucket names must not be formatted as an IP address (for example, 192.168.5.4).
        return !ParseUrl.isIpAddr(hostLabel);
    }

    private static boolean isAlphanumeric(char c) {
        return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
    }

    private static boolean isValidBucketSegmentChar(char c) {
        return isAlphanumeric(c) || c == '-';
    }
}
