/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.transform;

import software.amazon.smithy.model.traits.EnumTrait;

/**
 * Options that can be enabled when changing shape types.
 */
public enum ChangeShapeTypeOption {
    /**
     * Enables synthesizing enum names when changing a string shape to an enum shape and the
     * string shape's {@link EnumTrait} doesn't have names.
     */
    SYNTHESIZE_ENUM_NAMES;

    boolean hasFeature(ChangeShapeTypeOption[] haystack) {
        for (ChangeShapeTypeOption feature : haystack) {
            if (feature == this) {
                return true;
            }
        }
        return false;
    }
}
