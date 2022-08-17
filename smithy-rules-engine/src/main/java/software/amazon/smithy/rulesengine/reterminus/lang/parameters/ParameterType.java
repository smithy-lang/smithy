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

package software.amazon.smithy.rulesengine.reterminus.lang.parameters;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.reterminus.error.RuleError;

public enum ParameterType {
    STRING, BOOLEAN;

    private static final String TYPE = "type";

    public static ParameterType fromNode(StringNode node) throws RuleError {
        String value = node.getValue();
        if (value.equalsIgnoreCase("String")) {
            return STRING;
        } else if (value.equalsIgnoreCase("Boolean")) {
            return BOOLEAN;
        }
        throw new RuleError(new SourceException(String.format("Unexpected parameter type `%s`. Expected `String` or "
                                                              + "`Boolean`.", value), node));
    }

    @Override
    public String toString() {
        switch (this) {
            case STRING:
                return "String";
            case BOOLEAN:
                return "Boolean";
            default:
                throw new IllegalArgumentException();
        }
    }
}
