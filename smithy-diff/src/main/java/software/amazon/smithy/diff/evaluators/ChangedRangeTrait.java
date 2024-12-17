/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.math.BigDecimal;
import java.util.Optional;
import software.amazon.smithy.model.traits.RangeTrait;

/**
 * Detects when the Range trait is made more restrictive by either
 * raising the min or lowering the max.
 */
public final class ChangedRangeTrait extends AbstractLengthAndRangeValidator<RangeTrait> {
    @Override
    protected Class<RangeTrait> getTraitType() {
        return RangeTrait.class;
    }

    @Override
    protected Optional<BigDecimal> getMin(RangeTrait t) {
        return t.getMin();
    }

    @Override
    protected Optional<BigDecimal> getMax(RangeTrait t) {
        return t.getMax();
    }
}
