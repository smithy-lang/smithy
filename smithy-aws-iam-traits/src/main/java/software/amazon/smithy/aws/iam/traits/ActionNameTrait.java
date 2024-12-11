/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

/**
 * Use the {@code @iamAction} trait's {@code name} property instead.
 *
 * @deprecated As of release 1.44.0, replaced by {@link IamActionTrait#resolveActionName}.
 */
@Deprecated
public final class ActionNameTrait extends StringTrait {

    public static final ShapeId ID = ShapeId.from("aws.iam#actionName");

    public ActionNameTrait(String action) {
        super(ID, action, SourceLocation.NONE);
    }

    public ActionNameTrait(String action, FromSourceLocation sourceLocation) {
        super(ID, action, sourceLocation);
    }

    public static final class Provider extends StringTrait.Provider<ActionNameTrait> {
        public Provider() {
            super(ID, ActionNameTrait::new);
        }
    }
}
