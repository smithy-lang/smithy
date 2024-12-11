/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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
