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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.functions.Function;
import software.amazon.smithy.rulesengine.language.syntax.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.functions.LibraryFunction;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for URI encoding a string.
 */
@SmithyUnstableApi
public final class UriEncode implements FunctionDefinition {
    public static final String ID = "uriEncode";

    private static final Map<String, String> ENCODING_REPLACEMENTS = MapUtils.of(
            "+", "%20",
            "*", "%2A",
            "%7E", "~"
    );

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

    public static Function ofExpression(Expression expression) {
        return LibraryFunction.ofExpressions(new UriEncode(), expression);
    }
}
