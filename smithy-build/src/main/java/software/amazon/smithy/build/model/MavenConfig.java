/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.model;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class MavenConfig implements ToSmithyBuilder<MavenConfig> {

    private final Set<String> dependencies;
    private final Set<MavenRepository> repositories;

    private MavenConfig(Builder builder) {
        this.dependencies = builder.dependencies.copy();
        this.repositories = builder.repositories.copy();
    }

    public static MavenConfig fromNode(Node node) {
        MavenConfig.Builder builder = builder();
        node.expectObjectNode()
                .warnIfAdditionalProperties(ListUtils.of("dependencies", "repositories"))
                .getArrayMember("dependencies", StringNode::getValue, builder::dependencies)
                .getArrayMember("repositories", MavenRepository::fromNode, builder::repositories);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the repositories.
     *
     * @return Returns the repositories in an insertion ordered set.
     */
    public Set<MavenRepository> getRepositories() {
        return repositories;
    }

    /**
     * Gets the dependencies.
     *
     * @return Returns the dependencies in an insertion ordered set.
     */
    public Set<String> getDependencies() {
        return dependencies;
    }

    public MavenConfig merge(MavenConfig other) {
        MavenConfig.Builder builder = toBuilder();
        builder.dependencies.get().addAll(other.getDependencies());

        if (other.repositories != null) {
            builder.repositories.get().addAll(other.repositories);
        }

        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        return builder().repositories(repositories).dependencies(dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dependencies, repositories);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof MavenConfig)) {
            return false;
        }

        MavenConfig other = (MavenConfig) obj;
        return dependencies.equals(other.dependencies) && Objects.equals(repositories, other.repositories);
    }

    public static final class Builder implements SmithyBuilder<MavenConfig> {
        private final BuilderRef<Set<String>> dependencies = BuilderRef.forOrderedSet();
        private final BuilderRef<Set<MavenRepository>> repositories = BuilderRef.forOrderedSet();

        private Builder() {}

        @Override
        public MavenConfig build() {
            return new MavenConfig(this);
        }

        public Builder dependencies(Collection<String> dependencies) {
            this.dependencies.clear();
            this.dependencies.get().addAll(dependencies);
            return this;
        }

        public Builder repositories(Collection<MavenRepository> repositories) {
            this.repositories.clear();
            if (repositories != null) {
                this.repositories.get().addAll(repositories);
            }
            return this;
        }
    }
}
