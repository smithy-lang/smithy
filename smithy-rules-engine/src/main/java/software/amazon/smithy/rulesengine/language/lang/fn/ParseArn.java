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

package software.amazon.smithy.rulesengine.language.lang.fn;

import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.impl.Arn;
import software.amazon.smithy.rulesengine.language.lang.Identifier;
import software.amazon.smithy.rulesengine.language.lang.expr.Expr;
import software.amazon.smithy.rulesengine.language.visit.FnVisitor;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class ParseArn extends SingleArgFn<Type.Str> {
    public static final String ID = "aws.parseArn";
    public static final Identifier PARTITION = Identifier.of("partition");
    public static final Identifier SERVICE = Identifier.of("service");
    public static final Identifier REGION = Identifier.of("region");
    public static final Identifier ACCOUNT_ID = Identifier.of("accountId");
    private static final Identifier RESOURCE_ID = Identifier.of("resourceId");

    public ParseArn(FnNode fnNode) {
        super(fnNode, Type.str());
    }

    public static ParseArn ofExprs(Expr expr) {
        return new ParseArn(FnNode.ofExprs(ID, expr));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitParseArn(this);
    }

    @Override
    protected Value evalArg(Value arg) {
        String value = arg.expectString();
        Optional<Arn> arnOpt = Arn.parse(value);
        return arnOpt.map(arn ->
                (Value) Value.record(MapUtils.of(
                        PARTITION, Value.str(arn.partition()),
                        SERVICE, Value.str(arn.service()),
                        REGION, Value.str(arn.region()),
                        ACCOUNT_ID, Value.str(arn.accountId()),
                        RESOURCE_ID, Value.array(arn.resource().stream()
                                .map(v -> (Value) Value.str(v))
                                .collect(Collectors.toList()))
                ))
        ).orElse(new Value.None());
    }

    @Override
    protected Type typecheckArg(Scope<Type> scope, Type.Str arg) {
        return Type.optional(new Type.Record(MapUtils.of(
                PARTITION, Type.str(),
                SERVICE, Type.str(),
                REGION, Type.str(),
                ACCOUNT_ID, Type.str(),
                RESOURCE_ID, Type.array(Type.str())
        )));
    }
}
