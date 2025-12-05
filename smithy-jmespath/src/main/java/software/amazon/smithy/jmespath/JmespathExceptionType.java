/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

public enum JmespathExceptionType {
    SYNTAX,
    INVALID_TYPE,
    INVALID_VALUE,
    UNKNOWN_FUNCTION,
    INVALID_ARITY,
    OTHER;

    /**
     * Returns the corresponding enum value for one of the identifiers used in
     * <a href="https://jmespath.org/specification.html#errors">the JMESPath specification</a>.
     * <p>
     * "syntax" is not listed in the specification, but it is used
     * in <a href="https://github.com/jmespath/jmespath.test">the compliance tests</a> to indicate invalid expressions.
     */
    public static JmespathExceptionType fromID(String id) {
        return valueOf(id.toUpperCase().replace('-', '_'));
    }
}
