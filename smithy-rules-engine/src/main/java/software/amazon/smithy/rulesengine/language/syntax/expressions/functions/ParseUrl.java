/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.ToExpression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * A rule-set function to parse a URI from a string.
 */
@SmithyUnstableApi
public final class ParseUrl extends LibraryFunction {
    public static final String ID = "parseURL";
    public static final Identifier SCHEME = Identifier.of("scheme");
    public static final Identifier AUTHORITY = Identifier.of("authority");
    public static final Identifier PATH = Identifier.of("path");
    public static final Identifier NORMALIZED_PATH = Identifier.of("normalizedPath");
    public static final Identifier IS_IP = Identifier.of("isIp");
    private static final Definition DEFINITION = new Definition();

    private ParseUrl(FunctionNode functionNode) {
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
     * Creates a {@link ParseUrl} function from the given expressions.
     *
     * @param arg1 the URI to parse.
     * @return The resulting {@link ParseUrl} function.
     */
    public static ParseUrl ofExpressions(ToExpression arg1) {
        return DEFINITION.createFunction(FunctionNode.ofExpressions(ID, arg1));
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link ParseUrl} function.
     */
    public static final class Definition implements FunctionDefinition {
        private final Type returnType;

        private Definition() {
            Map<Identifier, Type> types = new LinkedHashMap<>();
            types.put(SCHEME, Type.stringType());
            types.put(AUTHORITY, Type.stringType());
            types.put(PATH, Type.stringType());
            types.put(NORMALIZED_PATH, Type.stringType());
            types.put(IS_IP, Type.booleanType());
            returnType = Type.optionalType(Type.recordType(types));
        }

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
            return returnType;
        }

        @Override
        public Value evaluate(List<Value> arguments) {
            String url = arguments.get(0).expectStringValue().getValue();
            try {
                URL parsed = new URL(url);
                if (parsed.getQuery() != null) {
                    return Value.emptyValue();
                }

                boolean isIpAddr = false;
                String host = parsed.getHost();
                if (host.startsWith("[") && host.endsWith("]")) {
                    isIpAddr = true;
                }
                String[] dottedParts = host.split("\\.");
                if (dottedParts.length == 4) {
                    isIpAddr = true;
                    for (String dottedPart : dottedParts) {
                        try {
                            int value = Integer.parseInt(dottedPart);
                            if (value < 0 || value > 255) {
                                isIpAddr = false;
                            }
                        } catch (NumberFormatException ex) {
                            isIpAddr = false;
                        }
                    }
                }

                String path = parsed.getPath();
                String normalizedPath;
                if (StringUtils.isBlank(path)) {
                    normalizedPath = "/";
                } else {
                    StringBuilder builder = new StringBuilder();
                    if (!path.startsWith("/")) {
                        builder.append("/");
                    }
                    builder.append(path);
                    if (!path.endsWith("/")) {
                        builder.append("/");
                    }
                    normalizedPath = builder.toString();
                }

                Map<Identifier, Value> values = new LinkedHashMap<>();
                values.put(SCHEME, Value.stringValue(parsed.getProtocol()));
                values.put(AUTHORITY, Value.stringValue(parsed.getAuthority()));
                values.put(PATH, Value.stringValue(path));
                values.put(NORMALIZED_PATH, Value.stringValue(normalizedPath));
                values.put(IS_IP, Value.booleanValue(isIpAddr));
                return Value.recordValue(values);
            } catch (MalformedURLException e) {
                return Value.emptyValue();
            }
        }

        @Override
        public ParseUrl createFunction(FunctionNode functionNode) {
            return new ParseUrl(functionNode);
        }
    }
}
