/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * Use the {@code @iamAction} trait's {@code documentation} property instead.
 *
 * @deprecated As of release 1.44.0, replaced by {@link IamActionTrait#resolveActionDocumentation}.
 */
@Deprecated
public final class ActionPermissionDescriptionTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.iam#actionPermissionDescription");

    public ActionPermissionDescriptionTrait(String value) {
        super(ID, value, SourceLocation.NONE);
    }

    public ActionPermissionDescriptionTrait(String value, SourceLocation sourceLocation) {
        super(ID, value, sourceLocation);
    }

    public static final class Provider extends StringTrait.Provider<ActionPermissionDescriptionTrait> {
        public Provider() {
            super(ID, ActionPermissionDescriptionTrait::new);
        }
    }
}
