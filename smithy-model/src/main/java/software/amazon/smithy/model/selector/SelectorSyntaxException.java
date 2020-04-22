/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.selector;

/**
 * Exception thrown when a selector expression is invalid.
 */
public final class SelectorSyntaxException extends RuntimeException {
    SelectorSyntaxException(String message, String expression, int pos) {
        super(createMessage(message, expression, pos));
    }

    private static String createMessage(String message, String expression, int pos) {
        String result = "Syntax error at character " + pos + " of " + expression.length();
        if (pos <= expression.length()) {
            result += ", near `" + expression.substring(pos) + "`";
        }
        return result + ": " + message + "; expression: " + expression;
    }
}
