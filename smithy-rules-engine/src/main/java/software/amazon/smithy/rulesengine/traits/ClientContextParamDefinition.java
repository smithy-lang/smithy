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
import java.util.Optional;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * A service client context parameter definition.
 */
@SmithyUnstableApi
public final class ClientContextParamDefinition implements ToSmithyBuilder<ClientContextParamDefinition> {
    private final ShapeType type;
    private final String documentation;

    private ClientContextParamDefinition(Builder builder) {
        this.type = SmithyBuilder.requiredState("type", builder.type);
        this.documentation = builder.documentation;
    }

    public static Builder builder() {
        return new Builder();
    }

    public ShapeType getType() {
        return type;
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    public Builder toBuilder() {
        return builder()
                .type(type)
                .documentation(documentation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getType(), getDocumentation());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClientContextParamDefinition that = (ClientContextParamDefinition) o;
        return getType() == that.getType() && Objects.equals(getDocumentation(), that.getDocumentation());
    }

    public static final class Builder implements SmithyBuilder<ClientContextParamDefinition> {
        private ShapeType type;
        private String documentation;

        private Builder() {
        }

        public Builder type(ShapeType type) {
            this.type = type;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public ClientContextParamDefinition build() {
            return new ClientContextParamDefinition(this);
        }
    }
}
