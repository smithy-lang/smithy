/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import software.amazon.smithy.rulesengine.language.RulesVersion;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Splits a string by a delimiter into parts.
 *
 * <p>The split function divides a string into an array of substrings based on a non-empty delimiter.
 * The behavior is controlled by the limit parameter:
 * <ul>
 *   <li>limit = 0: Split all occurrences (unlimited)</li>
 *   <li>limit = 1: No split performed (returns original string as single element)</li>
 *   <li>limit > 1: Split into at most 'limit' parts (performs limit-1 splits)</li>
 * </ul>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code split("a--b--c", "--", 0)} returns {@code ["a", "b", "c"]}</li>
 *   <li>{@code split("a--b--c", "--", 2)} returns {@code ["a", "b--c"]}</li>
 *   <li>{@code split("--b--", "--", 0)} returns {@code ["", "b", ""]}</li>
 *   <li>{@code split("abc", "x", 0)} returns {@code ["abc"]}</li>
 *   <li>{@code split("", "--", 0)} returns {@code [""]}</li>
 *   <li>{@code split("----", "--", 0)} returns {@code ["", "", ""]}</li>
 *   <li>{@code split("a--b--c--d", "--", 3)} returns {@code ["a", "b", "c--d"]}</li>
 *   <li>{@code split("prefix", "--", 0)} returns {@code ["prefix"]}</li>
 *   <li>{@code split("--", "--", 0)} returns {@code ["", ""]}</li>
 *   <li>{@code split("a-b-c", "--", 0)} returns {@code ["a-b-c"]}</li>
 *   <li>{@code split("mybucket", "--", 1)} returns {@code ["mybucket"]}</li>
 *   <li>{@code split("--x-s3--azid--suffix", "--", 0)} returns {@code ["", "x-s3", "azid", "suffix"]}</li>
 *   <li>{@code split("--x-s3--azid--suffix", "--", 4)} returns {@code ["", "x-s3", "azid", "suffix"]}</li>
 *   <li>{@code split("--x-s3--azid--suffix", "--", 2)} returns {@code ["", "x-s3--azid--suffix"]}</li>
 * </ul>
 */
@SmithyUnstableApi
public final class Split extends LibraryFunction {
    public static final String ID = "split";
    private static final Definition DEFINITION = new Definition();

    private Split(FunctionNode functionNode) {
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
     * Creates a {@link Split} function from the given expressions.
     *
     * @param string the string to split.
     * @param delimiter the delimiter.
     * @param limit the split limit (0 for unlimited, positive for max parts).
     * @return The resulting {@link Split} function.
     */
    public static Split ofExpressions(ToExpression string, ToExpression delimiter, ToExpression limit) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, string, delimiter, limit));
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    @Override
    public RulesVersion availableSince() {
        return RulesVersion.V1_1;
    }

    /**
     * A {@link FunctionDefinition} for the {@link Split} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public int getCost() {
            return 60;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(Type.stringType(), Type.stringType(), Type.integerType());
        }

        @Override
        public Type getReturnType() {
            return Type.arrayType(Type.stringType());
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String input = arguments.get(0).expectStringValue().getValue();
            String delimiter = arguments.get(1).expectStringValue().getValue();
            int limit = arguments.get(2).expectIntegerValue().getValue();

            List<String> parts = split(input, delimiter, limit);

            List<Value> values = new ArrayList<>();
            for (String part : parts) {
                values.add(Value.stringValue(part));
            }

            return Value.arrayValue(values);
        }

        @Override
        public Split createFunction(FunctionNode functionNode) {
            return new Split(functionNode);
        }
    }

    /**
     * Split a string by a delimiter.
     *
     * @param value the string to split (must not be null).
     * @param delimiter the delimiter to split by (must not be null or empty).
     * @param limit controls the split behavior:
     *              0 = unlimited splits,
     *              1 = no split (return original),
     *              n > 1 = split into at most n parts by performing up to n-1 split operations.
     * @return a non-null list of parts (never empty; returns [""] for empty input).
     * @throws NullPointerException if value or delimiter is null.
     * @throws IllegalArgumentException if delimiter is empty, or if limit is negative.
     */
    public static List<String> split(String value, String delimiter, int limit) {
        Objects.requireNonNull(value, "Split value cannot be null");
        Objects.requireNonNull(delimiter, "Split delimiter cannot be null");
        if (delimiter.isEmpty()) {
            throw new IllegalArgumentException("Split delimiter cannot be empty");
        } else if (limit < 0) {
            throw new IllegalArgumentException("Split limit cannot be negative, but given: " + limit);
        } else if (value.isEmpty() || limit == 1) {
            return Collections.singletonList(value);
        }

        final int delimLen = delimiter.length();
        final List<String> result = (limit > 1) ? new ArrayList<>(limit) : new ArrayList<>();
        int start = 0;
        int splits = 0;

        while (limit == 0 || splits < limit - 1) {
            int pos = value.indexOf(delimiter, start);
            if (pos == -1) {
                break;
            }
            result.add(value.substring(start, pos));
            start = pos + delimLen;
            splits++;
        }

        // Add remainder (or entire string if no delimiter found)
        result.add(value.substring(start));
        return result;
    }
}
