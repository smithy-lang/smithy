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

package software.amazon.smithy.rulesengine.reterminus.lang.fn;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Value;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.Parameter;
import software.amazon.smithy.rulesengine.reterminus.visit.FnVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public class UriEncode extends SingleArgFn<Type.Str> {

    public static final String ID = "uriEncode";
    private static final String[] ENCODED_CHARACTERS = new String[]{"+", "*", "%7E"};
    private static final String[] ENCODED_CHARACTERS_REPLACEMENTS = new String[]{"%20", "%2A", "~"};

    public UriEncode(FnNode node) {
        super(node, Type.str());
    }

    public static UriEncode ofExprs(Expr expr) {
        return new UriEncode(FnNode.ofExprs(ID, expr));
    }

    public static UriEncode fromParam(Parameter param) {
        return UriEncode.ofExprs(param.expr());
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitUriEncode(this);
    }

    @Override
    protected Value evalArg(Value arg) {
        String url = arg.expectString();
        try {
            String encoded = URLEncoder.encode(url, "UTF-8");
            for (int i = 0; i < ENCODED_CHARACTERS.length; i++) {
                encoded = encoded.replace(ENCODED_CHARACTERS[i], ENCODED_CHARACTERS_REPLACEMENTS[i]);
            }
            return Value.str(encoded);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Type typecheckArg(Scope<Type> scope, Type.Str arg) {
        return Type.str();
    }
}
