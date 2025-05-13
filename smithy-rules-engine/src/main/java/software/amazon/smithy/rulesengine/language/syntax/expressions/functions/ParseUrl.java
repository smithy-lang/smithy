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
    public static final Definition DEFINITION = new Definition();

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

                boolean isIpAddr = isIpAddr(parsed.getHost());
                String path = parsed.getPath();
                String normalizedPath = normalizePath(path);

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

    /**
     * Checks if a host is an IP address for use with endpoint rules.
     *
     * @param host Host to check.
     * @return true if it is an IP address.
     */
    public static boolean isIpAddr(String host) {
        if (host == null || host.length() < 2) {
            return false;
        }

        // Simple check for IPv6 (enclosed in square brackets)
        if (host.charAt(0) == '[' && host.charAt(host.length() - 1) == ']') {
            return true;
        }

        int from = 0;
        int segments = 0;
        boolean done = false;
        while (!done) {
            int index = host.indexOf('.', from);
            if (index == -1) {
                if (segments != 3) {
                    // E.g., 123.com
                    return false;
                }
                index = host.length();
                done = true;
            } else if (segments == 3) {
                // E.g., 1.2.3.4.5
                return false;
            }
            int length = index - from;
            if (length == 1) {
                char ch0 = host.charAt(from);
                if (ch0 < '0' || ch0 > '9') {
                    return false;
                }
            } else if (length == 2) {
                char ch0 = host.charAt(from);
                char ch1 = host.charAt(from + 1);
                if ((ch0 <= '0' || ch0 > '9') || (ch1 < '0' || ch1 > '9')) {
                    return false;
                }
            } else if (length == 3) {
                char ch0 = host.charAt(from);
                char ch1 = host.charAt(from + 1);
                char ch2 = host.charAt(from + 2);
                if ((ch0 <= '0' || ch0 > '9')
                        || (ch1 < '0' || ch1 > '9')
                        || (ch2 < '0' || ch2 > '9')) {
                    return false;
                }
                // This is a heuristic; We are intentionally not checking for the range 000-255.
            } else {
                return false;
            }
            from = index + 1;
            segments += 1;
        }
        return true;
    }

    /**
     * Normalizes a path string according to the ParseURL function.
     *
     * @param path Path string to normalize.
     * @return the normalized path.
     */
    public static String normalizePath(String path) {
        if (StringUtils.isBlank(path)) {
            return "/";
        } else {
            StringBuilder builder = new StringBuilder();
            if (!path.startsWith("/")) {
                builder.append("/");
            }
            builder.append(path);
            if (!path.endsWith("/")) {
                builder.append("/");
            }
            return builder.toString();
        }
    }
}
