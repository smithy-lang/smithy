/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
