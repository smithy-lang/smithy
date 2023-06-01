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

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.type.BooleanType;
import software.amazon.smithy.rulesengine.language.evaluation.type.StringType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public enum ParameterType {
    STRING, BOOLEAN;

    public static ParameterType fromNode(StringNode node) throws RuleError {
        String value = node.getValue();
        if (value.equalsIgnoreCase("String")) {
            return STRING;
        }
        if (value.equalsIgnoreCase("Boolean")) {
            return BOOLEAN;
        }
        throw new RuleError(new SourceException(
                String.format("Unexpected parameter type `%s`. Expected `String` or `Boolean`.", value), node));
    }

    public static ParameterType fromNode(Node node) throws RuleError {
        if (node.isStringNode()) {
            return STRING;
        }
        if (node.isBooleanNode()) {
            return BOOLEAN;
        }
        throw new RuleError(new SourceException(
                String.format("Unexpected parameter type `%s`. Expected a string or boolean.", node.getType()), node));
    }

    public static ParameterType fromType(Type type) {
        if (type instanceof StringType) {
            return STRING;
        }
        if (type instanceof BooleanType) {
            return BOOLEAN;
        }
        throw new RuntimeException(
                String.format("Unexpected parameter type `%s`. Expected a string or boolean.", type));
    }

    public static ParameterType fromShapeType(ShapeType type) {
        if (type == ShapeType.STRING) {
            return STRING;
        }
        if (type == ShapeType.BOOLEAN) {
            return BOOLEAN;
        }
        throw new RuntimeException(
                String.format("Unexpected parameter type `%s`. Expected string or boolean.", type));
    }

    @Override
    public String toString() {
        return this == STRING ? "String" : "Boolean";
    }
}
