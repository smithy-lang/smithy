/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.selector;

/**
 * Exception thrown when a selector expression is invalid.
 */
public final class SelectorSyntaxException extends SelectorException {
    SelectorSyntaxException(String message, String expression, int pos, int line, int column) {
        super(createMessage(message, expression, pos, line, column));
    }

    private static String createMessage(String message, String expression, int pos, int line, int column) {
        String result = "Syntax error at line " + line + " column " + column;

        if (pos <= expression.length()) {
            result += ", near `" + expression.substring(pos) + "`";
        }

        return result + ": " + message + "; expression: " + expression;
    }
}
