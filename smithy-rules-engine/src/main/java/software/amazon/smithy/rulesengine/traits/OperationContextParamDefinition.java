/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.traits;

import java.util.Objects;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An operation context parameter definition.
 */
@SmithyUnstableApi
public final class OperationContextParamDefinition implements ToSmithyBuilder<OperationContextParamDefinition> {
    private final String path;

    private OperationContextParamDefinition(Builder builder) {
        this.path = SmithyBuilder.requiredState("path", builder.path);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getPath() {
        return path;
    }

    @Override
    public Builder toBuilder() {
        return builder().path(path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OperationContextParamDefinition that = (OperationContextParamDefinition) o;
        return getPath().equals(that.getPath());
    }

    public static final class Builder implements SmithyBuilder<OperationContextParamDefinition> {
        private String path;

        private Builder() {}

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public OperationContextParamDefinition build() {
            return new OperationContextParamDefinition(this);
        }
    }
}
