/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

import software.amazon.smithy.rulesengine.language.error.InnerParseError;

/**
 * The "empty" type.
 */
public final class EmptyType extends AbstractType {
    EmptyType() {}

    @Override
    public EmptyType expectEmptyType() throws InnerParseError {
        return this;
    }

    @Override
    public String toString() {
        return "EmptyType[]";
    }
}
