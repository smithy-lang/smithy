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

package software.amazon.smithy.aws.traits.iam;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * Defines the description of what providing access to an operation entails.
 */
public final class ActionPermissionDescriptionTrait extends StringTrait {
    private static final String TRAIT = "aws.iam#actionPermissionDescription";

    private ActionPermissionDescriptionTrait(String value, SourceLocation sourceLocation) {
        super(TRAIT, value, sourceLocation);
    }

    public static final class Provider extends StringTrait.Provider<ActionPermissionDescriptionTrait> {
        public Provider() {
            super(TRAIT, ActionPermissionDescriptionTrait::new);
        }
    }
}
