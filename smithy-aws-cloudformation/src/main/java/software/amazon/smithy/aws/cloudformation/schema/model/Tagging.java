/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.schema.model;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Contains extracted resource tagging information.
 */
public final class Tagging implements ToSmithyBuilder<Tagging> {
    private final boolean taggable;
    private final boolean tagOnCreate;
    private final boolean tagUpdatable;
    private final String tagProperty;
    private final boolean cloudFormationSystemTags;
    private final Set<String> permissions;

    private Tagging(Builder builder) {
        taggable = builder.taggable;
        tagOnCreate = builder.tagOnCreate;
        tagUpdatable = builder.tagUpdatable;
        cloudFormationSystemTags = builder.cloudFormationSystemTags;
        tagProperty = builder.tagProperty;
        this.permissions = SetUtils.orderedCopyOf(builder.permissions);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if the resource is taggable.
     *
     * @return true if the resource is taggable.
     */
    public boolean getTaggable() {
        return taggable;
    }

    /**
     * Returns true if resource tags can be applied on create.
     *
     * @return true if resource tags can be applied on create.
     */
    public boolean getTagOnCreate() {
        return tagOnCreate;
    }

    /**
     * Returns true if resource tags can be updated after create.
     *
     * @return true if resource tags can be updated after create.
     */
    public boolean getTagUpdatable() {
        return tagUpdatable;
    }

    /**
     * Returns true if the resource supports CloudFormation system tags.
     *
     * @return true if the resource supports CloudFormation system tags.
     */
    public boolean getCloudFormationSystemTags() {
        return cloudFormationSystemTags;
    }

    /**
     * Returns the name of the tag property.
     *
     * @return the name of the tag property.
     */
    public String getTagProperty() {
        return tagProperty;
    }

    /**
     * Returns the set of permissions required to interact with this resource's tags.
     *
     * @return the set of permissions.
     */
    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .taggable(taggable)
                .tagOnCreate(tagOnCreate)
                .tagUpdatable(tagUpdatable)
                .cloudFormationSystemTags(cloudFormationSystemTags)
                .tagProperty(tagProperty)
                .permissions(permissions);
    }

    public static final class Builder implements SmithyBuilder<Tagging> {
        private boolean taggable;
        private boolean tagOnCreate;
        private boolean tagUpdatable;
        private boolean cloudFormationSystemTags;
        private String tagProperty;
        private final Set<String> permissions = new TreeSet<>();

        @Override
        public Tagging build() {
            return new Tagging(this);
        }

        public Builder taggable(boolean taggable) {
            this.taggable = taggable;
            return this;
        }

        public Builder tagOnCreate(boolean tagOnCreate) {
            this.tagOnCreate = tagOnCreate;
            return this;
        }

        public Builder tagUpdatable(boolean tagUpdatable) {
            this.tagUpdatable = tagUpdatable;
            return this;
        }

        public Builder cloudFormationSystemTags(boolean cloudFormationSystemTags) {
            this.cloudFormationSystemTags = cloudFormationSystemTags;
            return this;
        }

        public Builder tagProperty(String tagProperty) {
            this.tagProperty = tagProperty;
            return this;
        }

        public Builder permissions(Collection<String> permissions) {
            this.permissions.clear();
            this.permissions.addAll(permissions);
            return this;
        }

        public Builder addPermissions(Collection<String> permissions) {
            for (String permission : permissions) {
                addPermission(permission);
            }
            return this;
        }

        public Builder addPermission(String permission) {
            this.permissions.add(permission);
            return this;
        }

        public Builder clearPermissions() {
            this.permissions.clear();
            return this;
        }
    }
}
