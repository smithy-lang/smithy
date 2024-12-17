/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build.model;

import java.util.function.Function;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Transform configuration found in a projection.
 */
public final class TransformConfig {
    private final String name;
    private final ObjectNode args;

    private TransformConfig(Builder builder) {
        name = SmithyBuilder.requiredState("name", builder.name);
        args = builder.args;
    }

    public static TransformConfig fromNode(Node node) {
        TransformConfig.Builder builder = builder();
        node.expectObjectNode()
                .expectStringMember("name", builder::name)
                .getMember("args", Function.identity(), builder::args);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The name of the projection.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Gets the args.
     */
    public ObjectNode getArgs() {
        return args;
    }

    public static final class Builder implements SmithyBuilder<TransformConfig> {
        private String name;
        private ObjectNode args = Node.objectNode();

        private Builder() {}

        @Override
        public TransformConfig build() {
            return new TransformConfig(this);
        }

        /**
         * Sets the <strong>required</strong> name.
         *
         * @param name Name of the transform.
         * @return Returns the builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the args of the transform.
         *
         * <p>If an array is provided, the array is automatically converted
         * to an object with a key named "__args" that contains the array.
         * This is a backward compatibility shim for older versions of
         * Smithy Builder that only accepts a list of strings for
         * projection transforms.
         *
         * @param args Arguments to set.
         * @return Returns the builder.
         */
        public Builder args(Node args) {
            if (args.isArrayNode()) {
                this.args = Node.objectNode().withMember("__args", args);
            } else {
                this.args = args.expectObjectNode();
            }

            return this;
        }
    }
}
