/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringTrait;

public final class ConditionKeyValueTrait extends StringTrait {
    public static final ShapeId ID = ShapeId.from("aws.iam#conditionKeyValue");

    public ConditionKeyValueTrait(String conditionKey) {
        super(ID, conditionKey, SourceLocation.NONE);
    }

    public ConditionKeyValueTrait(String conditionKey, FromSourceLocation sourceLocation) {
        super(ID, conditionKey, sourceLocation);
    }

    public static final class Provider extends StringTrait.Provider<ConditionKeyValueTrait> {
        public Provider() {
            super(ID, ConditionKeyValueTrait::new);
        }
    }
}
