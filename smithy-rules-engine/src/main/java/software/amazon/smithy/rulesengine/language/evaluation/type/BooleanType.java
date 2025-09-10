/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.type;

import java.util.Optional;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;

/**
 * The "boolean" type.
 */
public final class BooleanType extends AbstractType {

    private static final Optional<Literal> ZERO = Optional.of(Literal.of(false));
    static final BooleanType INSTANCE = new BooleanType();

    BooleanType() {}

    @Override
    public BooleanType expectBooleanType() {
        return this;
    }

    @Override
    public Optional<Literal> getZeroValue() {
        return ZERO;
    }
}
