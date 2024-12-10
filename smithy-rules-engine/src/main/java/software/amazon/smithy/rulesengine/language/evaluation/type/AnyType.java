/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

import software.amazon.smithy.rulesengine.language.error.InnerParseError;

/**
 * The "any" type, which matches all other types.
 */
public final class AnyType extends AbstractType {
    AnyType() {}

    @Override
    public boolean isA(Type type) {
        return true;
    }

    @Override
    public AnyType expectAnyType() throws InnerParseError {
        return this;
    }

    @Override
    public String toString() {
        return "AnyType[]";
    }
}
