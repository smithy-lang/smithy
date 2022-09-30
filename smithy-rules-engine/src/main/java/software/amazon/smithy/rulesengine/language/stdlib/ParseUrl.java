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

package software.amazon.smithy.rulesengine.language.stdlib;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.Function;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * A rule-set function to parse a URI from a string.
 */
@SmithyUnstableApi
public class ParseUrl extends FunctionDefinition {
    public static final String ID = "parseURL";
    public static final Identifier SCHEME = Identifier.of("scheme");
    public static final Identifier AUTHORITY = Identifier.of("authority");
    public static final Identifier PATH = Identifier.of("path");
    public static final Identifier NORMALIZED_PATH = Identifier.of("normalizedPath");
    public static final Identifier IS_IP = Identifier.of("isIp");


    @Override
    public String getId() {
        return "parseURL";
    }

    @Override
    public List<Type> getArguments() {
        return Collections.singletonList(Type.string());
    }

    public static Function ofExpression(Expression expression) {
        return LibraryFunction.ofExpressions(new ParseUrl(), expression);
    }

    @Override
    public Type getReturnType() {
        return Type.optional(Type.record(
                MapUtils.of(
                        SCHEME, Type.string(),
                        AUTHORITY, Type.string(),
                        PATH, Type.string(),
                        NORMALIZED_PATH, Type.string(),
                        IS_IP, Type.bool()
                )
        ));
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String url = arguments.get(0).expectString();
        try {
            URL parsed = new URL(url);
            String path = parsed.getPath();
            if (parsed.getQuery() != null) {
                System.out.println("empty query not supported");
                return Value.none();

            }
            boolean isIpAddr = false;
            String host = parsed.getHost();
            if (host.startsWith("[") && host.endsWith("]")) {
                isIpAddr = true;
            }
            String[] dottedParts = host.split("\\.");
            if (dottedParts.length == 4) {
                if (Arrays.stream(dottedParts).allMatch(part -> {
                    try {
                        int value = Integer.parseInt(part);
                        return value >= 0 && value <= 255;
                    } catch (NumberFormatException ex) {
                        return false;
                    }
                })) {
                    isIpAddr = true;
                }
            }
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
            return Value.record(MapUtils.of(
                    SCHEME, Value.string(parsed.getProtocol()),
                    AUTHORITY, Value.string(parsed.getAuthority()),
                    PATH, Value.string(path),
                    NORMALIZED_PATH, Value.string(normalizedPath.toString()),
                    IS_IP, Value.bool(isIpAddr)
            ));
        } catch (MalformedURLException e) {
            System.out.printf("invalid URL: %s%n", e);
            return Value.none();
        }
    }
}
