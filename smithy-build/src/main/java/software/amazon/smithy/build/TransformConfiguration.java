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

package software.amazon.smithy.build;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * Transform configuration found in a projection.
 */
public final class TransformConfiguration implements ToNode {
    private final String name;
    private final List<String> args;

    private TransformConfiguration(Builder builder) {
        name = SmithyBuilder.requiredState("name", builder.name);
        args = List.copyOf(builder.args);
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
    public List<String> getArgs() {
        return args;
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember("name", Node.from(getName()))
                .withMember("args", Node.fromStrings(args))
                .build();
    }

    public static final class Builder implements SmithyBuilder<TransformConfiguration> {
        private String name;
        private List<String> args = Collections.emptyList();

        private Builder() {}

        public TransformConfiguration build() {
            return new TransformConfiguration(this);
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
         * @param args Arguments to set.
         * @return Returns the builder.
         */
        public Builder args(List<String> args) {
            this.args = args;
            return this;
        }
    }
}
