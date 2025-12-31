/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath;

/**
 * Defines categories of JMESPath errors.
 * A superset of the types defined in <a href="https://jmespath.org/specification.html#errors">the specification.
 */
public enum JmespathExceptionType {
    /**
     * The "syntax" class of JMESPath errors.
     * <p>
     * "syntax" is not listed in the specification, but it is used
     * in <a href="https://github.com/jmespath/jmespath.test">the compliance tests</a> to indicate invalid expressions.
     */
    SYNTAX,

    /**
     * The "invalid-type" class of JMESPath errors.
     */
    INVALID_TYPE,

    /**
     * The "invalid-value" class of JMESPath errors.
     */
    INVALID_VALUE,

    /**
     * The "unknown-function" class of JMESPath errors.
     */
    UNKNOWN_FUNCTION,

    /**
     * The "invalid-arity" class of JMESPath errors.
     */
    INVALID_ARITY,

    /**
     * Any other error cause.
     */
    OTHER;

    /**
     * Returns the corresponding enum value for one of the identifiers used in
     * <a href="https://jmespath.org/specification.html#errors">the JMESPath specification</a>.
     */
    public static JmespathExceptionType fromID(String id) {
        return valueOf(id.toUpperCase().replace('-', '_'));
    }
}
