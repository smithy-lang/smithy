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

package software.amazon.smithy.aws.traits.auth;

import java.util.Set;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Adds AWS signature version 4 authentication to a service or operation.
 */
public final class SigV4Trait extends AbstractTrait implements ToSmithyBuilder<SigV4Trait> {

    public static final ShapeId ID = ShapeId.from("aws.auth#sigv4");
    private static final String NAME = "name";
    private static final String SINGLE_ENCODE_CANONICAL_PATH = "singleEncodeCanonicalPath";
    private static final Set<String> PROPERTIES = SetUtils.of(NAME, SINGLE_ENCODE_CANONICAL_PATH);

    private final String name;
    private final boolean singleEncodeCanonicalPath;

    private SigV4Trait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.name = SmithyBuilder.requiredState(NAME, builder.name);
        this.singleEncodeCanonicalPath = builder.singleEncodeCanonicalPath;
    }

    /**
     * @return Gets the service signing name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Returns true if the canonical path is encoded only once.
     */
    public boolean isSingleEncodeCanonicalPath() {
        return singleEncodeCanonicalPath;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .name(getName())
                .singleEncodeCanonicalPath(isSingleEncodeCanonicalPath())
                .sourceLocation(getSourceLocation());
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        builder.withMember(NAME, getName());

        if (singleEncodeCanonicalPath) {
            builder.withMember(SINGLE_ENCODE_CANONICAL_PATH, true);
        }

        return builder.build();
    }

    public static final class Builder extends AbstractTraitBuilder<SigV4Trait, Builder> {
        private String name;
        private boolean singleEncodeCanonicalPath;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder singleEncodeCanonicalPath(boolean singleEncodeCanonicalPath) {
            this.singleEncodeCanonicalPath = singleEncodeCanonicalPath;
            return this;
        }

        @Override
        public SigV4Trait build() {
            return new SigV4Trait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            ObjectNode objectNode = value.expectObjectNode();
            objectNode.warnIfAdditionalProperties(PROPERTIES);
            builder.name(objectNode.expectStringMember(NAME).getValue());
            builder.singleEncodeCanonicalPath(objectNode.getBooleanMemberOrDefault(SINGLE_ENCODE_CANONICAL_PATH));
            return builder.build();
        }
    }
}
