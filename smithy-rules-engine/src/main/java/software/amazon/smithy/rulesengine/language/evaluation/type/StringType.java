/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

import java.util.Optional;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

/**
 * The "string" type.
 */
public final class StringType extends AbstractType {

    private static final Optional<Literal> ZERO = Optional.of(Literal.of(""));
    static final StringType INSTANCE = new StringType();

    StringType() {}

    @Override
    public StringType expectStringType() {
        return this;
    }

    @Override
    public Optional<Literal> getZeroValue() {
        return ZERO;
    }
}
