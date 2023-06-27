/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for getting the substring of a string value.
 */
@SmithyUnstableApi
public final class Substring extends LibraryFunction {
    public static final String ID = "substring";
    private static final Definition DEFINITION = new Definition();

    private Substring(FunctionNode functionNode) {
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

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link Substring} function.
     */
    public static final class Definition implements FunctionDefinition {
        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Arrays.asList(Type.stringType(), Type.integerType(), Type.integerType(), Type.booleanType());
        }

        @Override
        public Type getReturnType() {
            return Type.optionalType(Type.stringType());
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String str = arguments.get(0).expectStringValue().getValue();
            int startIndex = arguments.get(1).expectIntegerValue().getValue();
            int stopIndex = arguments.get(2).expectIntegerValue().getValue();
            boolean reverse = arguments.get(3).expectBooleanValue().getValue();

            for (int i = 0; i < str.length(); i++) {
                char ch = str.charAt(i);
                if (!(ch <= 127)) {
                    return Value.emptyValue();
                }
            }

            if (startIndex >= stopIndex || str.length() < stopIndex) {
                return Value.emptyValue();
            }

            if (!reverse) {
                return Value.stringValue(str.substring(startIndex, stopIndex));
            } else {
                int revStart = str.length() - stopIndex;
                int revStop = str.length() - startIndex;
                return Value.stringValue(str.substring(revStart, revStop));
            }
        }

        @Override
        public Substring createFunction(FunctionNode functionNode) {
            return new Substring(functionNode);
        }
    }
}
