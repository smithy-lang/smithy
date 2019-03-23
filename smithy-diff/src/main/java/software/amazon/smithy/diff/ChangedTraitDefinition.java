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

package software.amazon.smithy.diff;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.TraitDefinition;

/**
 * Represents a trait definition change.
 */
public final class ChangedTraitDefinition implements FromSourceLocation {
    private final TraitDefinition oldDefinition;
    private final TraitDefinition newDefinition;

    ChangedTraitDefinition(TraitDefinition oldDefinition, TraitDefinition newDefinition) {
        this.oldDefinition = oldDefinition;
        this.newDefinition = newDefinition;
    }

    /**
     * Gets the old trait definition value.
     *
     * @return Gets the old trait definition.
     */
    public TraitDefinition getOldDefinition() {
        return oldDefinition;
    }

    /**
     * Gets the new trait definition value.
     *
     * @return Gets the new trait definition.
     */
    public TraitDefinition getNewDefinition() {
        return newDefinition;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return newDefinition.getSourceLocation();
    }
}
