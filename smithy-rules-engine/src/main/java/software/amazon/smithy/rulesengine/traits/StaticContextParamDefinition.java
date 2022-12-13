/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.traits;

import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An operation static context parameter definition.
 */
@SmithyUnstableApi
public final class StaticContextParamDefinition implements ToSmithyBuilder<StaticContextParamDefinition> {
    private final Node value;

    private StaticContextParamDefinition(Builder builder) {
        this.value = SmithyBuilder.requiredState("value", builder.value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Node getValue() {
        return value;
    }

    @Override
    public Builder toBuilder() {
        return builder().value(value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StaticContextParamDefinition that = (StaticContextParamDefinition) o;
        return getValue().equals(that.getValue());
    }

    public static final class Builder implements SmithyBuilder<StaticContextParamDefinition> {
        private Node value;

        private Builder() {
        }

        public Builder value(Node value) {
            this.value = value;
            return this;
        }

        public StaticContextParamDefinition build() {
            return new StaticContextParamDefinition(this);
        }
    }
}
