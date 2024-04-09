/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.evaluation.type;

/**
 * The "integer" type.
 */
public final class IntegerType extends AbstractType {
    IntegerType() {}

    @Override
    public IntegerType expectIntegerType() {
        return this;
    }
}
