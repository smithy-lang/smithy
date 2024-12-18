/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
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

        private Builder() {}

        public Builder value(Node value) {
            this.value = value;
            return this;
        }

        public StaticContextParamDefinition build() {
            return new StaticContextParamDefinition(this);
        }
    }
}
