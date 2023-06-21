/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.evaluation.type;

/**
 * The "string" type.
 */
public final class StringType extends AbstractType {
    StringType() {}

    @Override
    public StringType expectStringType() {
        return this;
    }
}
