/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
