/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

/**
 * A comparator in a comparison expression.
 */
public enum ComparatorType {

    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS_THAN("<"),
    LESS_THAN_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_THAN_EQUAL(">=");

    private final String value;

    ComparatorType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
