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
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.Function;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for URI encoding a string.
 */
@SmithyUnstableApi
public final class UriEncode extends FunctionDefinition {

    public static final String ID = "uriEncode";
    private static final String[] ENCODED_CHARACTERS = new String[]{"+", "*", "%7E"};
    private static final String[] ENCODED_CHARACTERS_REPLACEMENTS = new String[]{"%20", "%2A", "~"};


    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<Type> getArguments() {
        return Collections.singletonList(Type.string());
    }

    @Override
    public Type getReturnType() {
        return Type.string();
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String url = arguments.get(0).expectString();
        try {
            String encoded = URLEncoder.encode(url, "UTF-8");
            for (int i = 0; i < ENCODED_CHARACTERS.length; i++) {
                encoded = encoded.replace(ENCODED_CHARACTERS[i], ENCODED_CHARACTERS_REPLACEMENTS[i]);
            }
            return Value.string(encoded);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static Function ofExpression(Expression expression) {
        return LibraryFunction.ofExpressions(new UriEncode(), expression);
    }
}
