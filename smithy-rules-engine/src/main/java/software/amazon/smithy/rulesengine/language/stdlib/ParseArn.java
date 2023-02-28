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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.eval.type.RecordType;
import software.amazon.smithy.rulesengine.language.eval.type.Type;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.impl.AwsArn;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.functions.Function;
import software.amazon.smithy.rulesengine.language.syntax.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.functions.LibraryFunction;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An aws rule-set function for parsing an AWS ARN into it's componenet parts.
 */
@SmithyUnstableApi
public final class ParseArn implements FunctionDefinition {
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
        return Collections.singletonList(Type.stringType());
    }

    @Override
    public Type getReturnType() {
        return Type.optionalType(new RecordType(MapUtils.of(
                PARTITION, Type.stringType(),
                SERVICE, Type.stringType(),
                REGION, Type.stringType(),
                ACCOUNT_ID, Type.stringType(),
                RESOURCE_ID, Type.arrayType(Type.stringType())
        )));
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String value = arguments.get(0).expectStringValue().getValue();
        Optional<AwsArn> arnOpt = AwsArn.parse(value);
        if (!arnOpt.isPresent()) {
            return Value.emptyValue();
        }

        AwsArn awsArn = arnOpt.get();
        List<Value> resourceId = new ArrayList<>();
        for (String resourceIdPart : awsArn.getResource()) {
            resourceId.add(Value.stringValue(resourceIdPart));
        }
        return Value.recordValue(MapUtils.of(
                PARTITION, Value.stringValue(awsArn.getPartition()),
                SERVICE, Value.stringValue(awsArn.getService()),
                REGION, Value.stringValue(awsArn.getRegion()),
                ACCOUNT_ID, Value.stringValue(awsArn.getAccountId()),
                RESOURCE_ID, Value.arrayValue(resourceId)));
    }

    public static Function ofExpression(Expression expression) {
        return LibraryFunction.ofExpressions(new ParseArn(), expression);
    }
}
