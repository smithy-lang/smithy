/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * A rule-set function to parse a URI from a string.
 */
@SmithyUnstableApi
public class ParseUrl extends LibraryFunction {
    public static final String ID = "parseURL";
    public static final Identifier SCHEME = Identifier.of("scheme");
    public static final Identifier AUTHORITY = Identifier.of("authority");
    public static final Identifier PATH = Identifier.of("path");
    public static final Identifier NORMALIZED_PATH = Identifier.of("normalizedPath");
    public static final Identifier IS_IP = Identifier.of("isIp");

    private static final Definition DEFINITION = new Definition();

    public ParseUrl(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    public static final class Definition implements FunctionDefinition {
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
            return Type.optionalType(Type.recordType(
                    MapUtils.of(
                            SCHEME, Type.stringType(),
                            AUTHORITY, Type.stringType(),
                            PATH, Type.stringType(),
                            NORMALIZED_PATH, Type.stringType(),
                            IS_IP, Type.booleanType()
                    )
            ));
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

                return Value.recordValue(MapUtils.of(
                        SCHEME, Value.stringValue(parsed.getProtocol()),
                        AUTHORITY, Value.stringValue(parsed.getAuthority()),
                        PATH, Value.stringValue(path),
                        NORMALIZED_PATH, Value.stringValue(normalizedPath),
                        IS_IP, Value.booleanValue(isIpAddr)
                ));
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
