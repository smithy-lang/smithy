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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.impl.AwsArn;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.Function;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.LibraryFunction;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An aws rule-set function for parsing an AWS ARN into it's componenet parts.
 */
@SmithyUnstableApi
public final class ParseArn extends FunctionDefinition {
    public static final String ID = "aws.parseArn";
    public static final Identifier PARTITION = Identifier.of("partition");
    public static final Identifier SERVICE = Identifier.of("service");
    public static final Identifier REGION = Identifier.of("region");
    public static final Identifier ACCOUNT_ID = Identifier.of("accountId");
    private static final Identifier RESOURCE_ID = Identifier.of("resourceId");


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
        return Type.optional(new Type.Record(MapUtils.of(
                PARTITION, Type.string(),
                SERVICE, Type.string(),
                REGION, Type.string(),
                ACCOUNT_ID, Type.string(),
                RESOURCE_ID, Type.array(Type.string())
        )));
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String value = arguments.get(0).expectString();
        Optional<AwsArn> arnOpt = AwsArn.parse(value);
        return arnOpt.map(awsArn ->
                (Value) Value.record(MapUtils.of(
                        PARTITION, Value.string(awsArn.partition()),
                        SERVICE, Value.string(awsArn.service()),
                        REGION, Value.string(awsArn.region()),
                        ACCOUNT_ID, Value.string(awsArn.accountId()),
                        RESOURCE_ID, Value.array(awsArn.resource().stream()
                                .map(v -> (Value) Value.string(v))
                                .collect(Collectors.toList()))
                ))
        ).orElse(new Value.None());
    }

    public static Function ofExpression(Expression expression) {
        return LibraryFunction.ofExpressions(new ParseArn(), expression);
    }
}
