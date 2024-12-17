/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.math.BigDecimal;
import java.util.Optional;
import software.amazon.smithy.model.traits.LengthTrait;

/**
 * Detects when the Length trait is made more restrictive by either
 * raising the min or lowering the max.
 */
public final class ChangedLengthTrait extends AbstractLengthAndRangeValidator<LengthTrait> {
    @Override
    protected Class<LengthTrait> getTraitType() {
        return LengthTrait.class;
    }

    @Override
    protected Optional<BigDecimal> getMin(LengthTrait t) {
        return t.getMin().map(BigDecimal::valueOf);
    }

    @Override
    protected Optional<BigDecimal> getMax(LengthTrait t) {
        return t.getMax().map(BigDecimal::valueOf);
    }
}
