/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An aws rule-set function for parsing an AWS ARN into it's componenet parts.
 */
@SmithyUnstableApi
public final class ParseArn extends LibraryFunction {
    public static final String ID = "aws.parseArn";

    public static final Identifier PARTITION = Identifier.of("partition");
    public static final Identifier SERVICE = Identifier.of("service");
    public static final Identifier REGION = Identifier.of("region");
    public static final Identifier ACCOUNT_ID = Identifier.of("accountId");
    private static final Identifier RESOURCE_ID = Identifier.of("resourceId");

    private static final Definition DEFINITION = new Definition();

    private ParseArn(FunctionNode functionNode) {
        super(DEFINITION, functionNode);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitLibraryFunction(DEFINITION, getArguments());
    }

    /**
     * A {@link FunctionDefinition} for the {@link ParseArn} function.
     */
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
            return Type.optionalType(Type.recordType(MapUtils.of(
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

        @Override
        public ParseArn createFunction(FunctionNode functionNode) {
            return new ParseArn(functionNode);
        }
    }
}
