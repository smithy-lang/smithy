/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for URI encoding a string.
 */
@SmithyUnstableApi
public final class UriEncode extends LibraryFunction {
    public static final String ID = "uriEncode";
    private static final Definition DEFINITION = new Definition();

    public UriEncode(FunctionNode functionNode) {
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
     * Creates a {@link UriEncode} function from the given expressions.
     *
     * @param arg1 the value to URI encode.
     * @return The resulting {@link UriEncode} function.
     */
    public static UriEncode ofExpressions(ToExpression arg1) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1));
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link UriEncode} function.
     */
    public static final class Definition implements FunctionDefinition {
        private static final Map<String, String> ENCODING_REPLACEMENTS = MapUtils.of(
                "+",
                "%20",
                "*",
                "%2A",
                "%7E",
                "~");

        private Definition() {}

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public List<Type> getArguments() {
            return Collections.singletonList(Type.stringType());
        }

        @Override
        public Type getReturnType() {
            return Type.stringType();
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String url = arguments.get(0).expectStringValue().getValue();
            try {
                String encoded = URLEncoder.encode(url, "UTF-8");
                for (Map.Entry<String, String> entry : ENCODING_REPLACEMENTS.entrySet()) {
                    encoded = encoded.replace(entry.getKey(), entry.getValue());
                }
                return Value.stringValue(encoded);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public UriEncode createFunction(FunctionNode functionNode) {
            return new UriEncode(functionNode);
        }
    }
}
