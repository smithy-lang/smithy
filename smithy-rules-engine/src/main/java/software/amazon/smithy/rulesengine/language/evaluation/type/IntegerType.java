/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

import java.util.Optional;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

/**
 * The "integer" type.
 */
public final class IntegerType extends AbstractType {

    private static final Optional<Literal> ZERO = Optional.of(Literal.of(0));
    static final IntegerType INSTANCE = new IntegerType();

    IntegerType() {}

    @Override
    public IntegerType expectIntegerType() {
        return this;
    }

    @Override
    public Optional<Literal> getZeroValue() {
        return ZERO;
    }
}
