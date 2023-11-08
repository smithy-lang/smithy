/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.iam.traits;

import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates properties of a Smithy operation as an IAM action.
 */
public final class IamActionTrait extends AbstractTrait
        implements ToSmithyBuilder<IamActionTrait> {
    public static final ShapeId ID = ShapeId.from("aws.iam#iamAction");

    private final String name;
    private final String documentation;
    private final String relativeDocumentation;
    private final List<String> requiredActions;
    private final ActionResources resources;
    private final List<String> createsResources;

    private IamActionTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        name = builder.name;
        documentation = builder.documentation;
        relativeDocumentation = builder.relativeDocumentation;
        requiredActions = builder.requiredActions.copy();
        resources = builder.resources;
        createsResources = builder.createsResources.copy();
    }

    /**
     * Get the AWS IAM resource name.
     *
     * @return Returns the name.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Gets the description of what granting the user permission to
     * invoke an operation would entail.
     *
     * @return Returns the documentation.
     */
    public Optional<String> getDocumentation() {
        return Optional.ofNullable(documentation);
    }

    /**
     * Gets the relative URL path for the action within a set of
     * IAM-related documentation.
     *
     * @return Returns the relative URL path to documentation.
     */
    public Optional<String> getRelativeDocumentation() {
        return Optional.ofNullable(relativeDocumentation);
    }

    /**
     * Gets other actions that the invoker must be authorized to
     * perform when executing the targeted operation.
     *
     * @return Returns the list of required actions.
     */
    public List<String> getRequiredActions() {
        return requiredActions;
    }

    /**
     * Gets the resources an IAM action can be authorized against.
     *
     * @return Returns the action's resources.
     */
    public Optional<ActionResources> getResources() {
        return Optional.ofNullable(resources);
    }

    /**
     * Gets the resources that performing this IAM action will create.
     *
     * @return Returns the resources created by the action.
     */
    public List<String> getCreatesResources() {
        return createsResources;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(IamActionTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public SmithyBuilder<IamActionTrait> toBuilder() {
        return builder().sourceLocation(getSourceLocation()).name(name);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            IamActionTrait result = new NodeMapper().deserialize(value, IamActionTrait.class);
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<IamActionTrait, Builder> {
        private String name;
        private String documentation;
        private String relativeDocumentation;
        private final BuilderRef<List<String>> requiredActions = BuilderRef.forList();
        private ActionResources resources;
        private final BuilderRef<List<String>> createsResources = BuilderRef.forList();

        private Builder() {}

        @Override
        public IamActionTrait build() {
            return new IamActionTrait(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder relativeDocumentation(String relativeDocumentation) {
            this.relativeDocumentation = relativeDocumentation;
            return this;
        }

        public Builder requiredActions(List<String> requiredActions) {
            clearRequiredActions();
            this.requiredActions.get().addAll(requiredActions);
            return this;
        }

        public Builder clearRequiredActions() {
            requiredActions.get().clear();
            return this;
        }

        public Builder addRequiredAction(String requiredAction) {
            requiredActions.get().add(requiredAction);
            return this;
        }

        public Builder removeRequiredAction(String requiredAction) {
            requiredActions.get().remove(requiredAction);
            return this;
        }

        public Builder resources(ActionResources resources) {
            this.resources = resources;
            return this;
        }

        public Builder createsResources(List<String> createsResources) {
            clearCreatesResources();
            this.createsResources.get().addAll(createsResources);
            return this;
        }

        public Builder clearCreatesResources() {
            createsResources.get().clear();
            return this;
        }

        public Builder addCreatesResource(String createsResource) {
            createsResources.get().add(createsResource);
            return this;
        }

        public Builder removeCreatesResource(String createsResource) {
            createsResources.get().remove(createsResource);
            return this;
        }
    }
}
