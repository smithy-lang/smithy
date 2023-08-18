/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.iam.traits;

import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;

public final class ServiceResolvedConditionKeysTrait extends StringListTrait {
    public static final ShapeId ID = ShapeId.from("aws.iam#serviceResolvedConditionKeys");

    public ServiceResolvedConditionKeysTrait(List<String> conditionKeys) {
        super(ID, conditionKeys, SourceLocation.NONE);
    }

    public ServiceResolvedConditionKeysTrait(List<String> conditionKeys, FromSourceLocation sourceLocation) {
        super(ID, conditionKeys, sourceLocation);
    }

    public static final class Provider extends StringListTrait.Provider<ServiceResolvedConditionKeysTrait> {
        public Provider() {
            super(ID, ServiceResolvedConditionKeysTrait::new);
        }
    }
}
