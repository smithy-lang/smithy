/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.Tagged;
import software.amazon.smithy.model.ToSmithyBuilder;

/**
 * An enum definition for the enum trait.
 */
public final class EnumConstantBody implements ToSmithyBuilder<EnumConstantBody>, Tagged {
    public static final String NAME = "name";
    public static final String DOCUMENTATION = "documentation";
    public static final String TAGS = "tags";

    private final String documentation;
    private final List<String> tags;
    private final String name;

    private EnumConstantBody(Builder builder) {
        documentation = builder.documentation;
        tags = new ArrayList<>(builder.tags);
        name = builder.name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public Builder toBuilder() {
        return builder().tags(tags).documentation(documentation).name(name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof EnumConstantBody)) {
            return false;
        }

        EnumConstantBody otherEnum = (EnumConstantBody) other;
        return Objects.equals(name, otherEnum.name)
                && Objects.equals(documentation, otherEnum.documentation)
                && tags.equals(otherEnum.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags, documentation);
    }

    /**
     * Builds a {@link EnumConstantBody}.
     */
    public static final class Builder implements SmithyBuilder<EnumConstantBody> {
        private String documentation;
        private String name;
        private final List<String> tags = new ArrayList<>();

        @Override
        public EnumConstantBody build() {
            return new EnumConstantBody(this);
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder tags(Collection<String> tags) {
            this.tags.clear();
            this.tags.addAll(tags);
            return this;
        }

        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        public Builder clearTags() {
            tags.clear();
            return this;
        }
    }
}
