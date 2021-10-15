/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.suppressions;

import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.SuppressTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * A suppression based on the {@link SuppressTrait}.
 */
final class TraitSuppression implements Suppression {

    private final ShapeId shape;
    private final SuppressTrait trait;

    TraitSuppression(ShapeId shape, SuppressTrait trait) {
        this.shape = shape;
        this.trait = trait;
    }

    @Override
    public boolean test(ValidationEvent event) {
        return event.getShapeId().filter(shape::equals).isPresent() && trait.getValues().contains(event.getId());
    }
}
