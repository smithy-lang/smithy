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

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

@SmithyUnstableApi
public final class ParameterReference implements ToSmithyBuilder<ParameterReference>, ToParameterReference {
    private final String name;
    private final String context;

    private ParameterReference(Builder builder) {
        this.name = SmithyBuilder.requiredState("name", builder.name);
        this.context = builder.context;
    }

    public static ParameterReference from(String reference) {
        String[] split = reference.split("\\.", 2);
        return from(split[0], split.length == 2 ? split[1] : null);
    }

    public static ParameterReference from(String name, String context) {
        Builder builder = builder().name(name);
        if (context != null) {
            builder.context(context);
        }
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public Optional<String> getContext() {
        return Optional.ofNullable(context);
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder()
                .name(getName());

        getContext().ifPresent(builder::context);

        return builder;
    }

    @Override
    public ParameterReference toParameterReference() {
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getContext());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParameterReference that = (ParameterReference) o;
        return getName().equals(that.getName()) && Objects.equals(getContext(), that.getContext());
    }

    @Override
    public String toString() {
        if (context == null) {
            return name;
        }
        return name + "." + context;
    }

    public static class Builder implements SmithyBuilder<ParameterReference> {
        private String name;
        private String context;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder context(String context) {
            this.context = context;
            return this;
        }

        @Override
        public ParameterReference build() {
            return new ParameterReference(this);
        }
    }
}
