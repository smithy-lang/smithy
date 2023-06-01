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

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.functions.Function;
import software.amazon.smithy.rulesengine.language.syntax.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.functions.LibraryFunction;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An AWS rule-set function for determining whether a given string can be promoted to an S3 virtual bucket host label.
 */
@SmithyUnstableApi
public class AwsIsVirtualHostableS3Bucket implements FunctionDefinition {
    public static final String ID = "aws.isVirtualHostableS3Bucket";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<Type> getArguments() {
        return Arrays.asList(Type.stringType(), Type.booleanType());
    }

    @Override
    public Type getReturnType() {
        return Type.booleanType();
    }

    @Override
    public Value evaluate(List<Value> arguments) {
        String hostLabel = arguments.get(0).expectStringValue().getValue();
        boolean allowDots = arguments.get(1).expectBooleanValue().getValue();
        if (allowDots) {
            return Value.booleanValue(
                    hostLabel.matches("[a-z\\d][a-z\\d\\-.]{1,61}[a-z\\d]")
                    && !hostLabel.matches("(\\d+\\.){3}\\d+") // don't allow ip address
                    && !hostLabel.matches(".*[.-]{2}.*") // don't allow names like bucket-.name or bucket.-name
            );
        } else {
            return Value.booleanValue(hostLabel.matches("[a-z\\d][a-z\\d\\-]{1,61}[a-z\\d]"));
        }
    }

    public static Function ofExpression(Expression input, boolean allowDots) {
        return LibraryFunction.ofExpressions(new AwsIsVirtualHostableS3Bucket(), input, Expression.of(allowDots));
    }
}
