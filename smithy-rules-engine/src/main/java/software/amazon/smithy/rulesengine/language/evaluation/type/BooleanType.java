/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

/**
 * The "boolean" type.
 */
public final class BooleanType extends AbstractType {
    BooleanType() {}

    @Override
    public BooleanType expectBooleanType() {
        return this;
    }
}
