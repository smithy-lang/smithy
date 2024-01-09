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

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates properties of a Smithy resource in AWS IAM.
 */
public final class IamResourceTrait extends AbstractTrait
        implements ToSmithyBuilder<IamResourceTrait> {
    public static final ShapeId ID = ShapeId.from("aws.iam#iamResource");

    private final String name;
    private final String relativeDocumentation;
    private final boolean disableConditionKeyInheritance;

    private IamResourceTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        name = builder.name;
        relativeDocumentation = builder.relativeDocumentation;
        disableConditionKeyInheritance = builder.disableConditionKeyInheritance;
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
     * Resolves the IAM resource name for the given resource. Uses the following
     * resolution order:
     *
     * <ol>
     *     <li>Value of the {@code @iamResource} trait's {@code name} property</li>
     *     <li>The resource's name</li>
     * </ol>
     *
     * @param resource the resource to resolve a name for.
     * @return The resolved resource name.
     */
    public static String resolveResourceName(ResourceShape resource) {
        return resource.getTrait(IamResourceTrait.class)
                .flatMap(IamResourceTrait::getName)
                .orElse(resource.getId().getName());
    }

    /**
     * Get the relative URL path that defines more information about the resource
     * within a set of IAM-related documentation.
     *
     * @return A relative URL to the documentation page.
     */
    public Optional<String> getRelativeDocumentation() {
        return Optional.ofNullable(relativeDocumentation);
    }

    /**
     * Gets if this IAM resource's condition keys are decoupled from
     * those of its parent resource(s).
     *
     * @return Returns true if condition key inheritance is disabled.
     */
    public boolean isDisableConditionKeyInheritance() {
        return disableConditionKeyInheritance;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(IamResourceTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public SmithyBuilder<IamResourceTrait> toBuilder() {
        return builder().sourceLocation(getSourceLocation()).name(name);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            IamResourceTrait result = new NodeMapper().deserialize(value, IamResourceTrait.class);
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<IamResourceTrait, Builder> {
        private String name;
        private String relativeDocumentation;
        private boolean disableConditionKeyInheritance;

        private Builder() {}

        @Override
        public IamResourceTrait build() {
            return new IamResourceTrait(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder relativeDocumentation(String relativeDocumentation) {
            this.relativeDocumentation = relativeDocumentation;
            return this;
        }

        public Builder disableConditionKeyInheritance(boolean disableConditionKeyInheritance) {
            this.disableConditionKeyInheritance = disableConditionKeyInheritance;
            return this;
        }
    }
}
