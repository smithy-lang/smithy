/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Applies condition keys to an operation or resource.
 */
public final class ConditionKeysTrait extends StringListTrait implements ToSmithyBuilder<ConditionKeysTrait> {
    public static final ShapeId ID = ShapeId.from("aws.iam#conditionKeys");
    private List<String> resolvedConditionKeys;

    public ConditionKeysTrait(List<String> keys, FromSourceLocation sourceLocation) {
        super(ID, keys, sourceLocation);
    }

    public ConditionKeysTrait(List<String> keys) {
        this(keys, SourceLocation.NONE);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the fully resolved condition key names based on the service's ARN namespace.
     *
     * @param service The service to resolve no-prefix condition key names to.
     * @return the resolved condition key names.
     */
    public List<String> resolveConditionKeys(ServiceShape service) {
        if (resolvedConditionKeys == null) {
            List<String> keys = new ArrayList<>();
            for (String value : getValues()) {
                keys.add(ConditionKeysIndex.resolveFullConditionKey(service, value));
            }
            resolvedConditionKeys = ListUtils.copyOf(keys);
        }
        return resolvedConditionKeys;
    }

    public static final class Provider extends StringListTrait.Provider<ConditionKeysTrait> {
        public Provider() {
            super(ID, ConditionKeysTrait::new);
        }
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).values(getValues());
    }

    public static final class Builder extends StringListTrait.Builder<ConditionKeysTrait, Builder> {
        private Builder() {}

        @Override
        public ConditionKeysTrait build() {
            return new ConditionKeysTrait(getValues(), getSourceLocation());
        }
    }
}
