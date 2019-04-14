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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.Tagged;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a Smithy protocol.
 */
public final class Protocol implements ToNode, ToSmithyBuilder<Protocol>, Tagged {
    private final String name;
    private final List<String> auth;
    private final List<String> tags;
    private Node node;

    private Protocol(Builder builder) {
        name = SmithyBuilder.requiredState("name", builder.name);
        auth = ListUtils.copyOf(builder.auth);
        tags = ListUtils.copyOf(builder.tags);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Gets the protocol name.
     *
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the ordered list of auth schemes supported by the protocol.
     *
     * @return Returns the auth schemes.
     */
    public List<String> getAuth() {
        return auth;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().name(name);
        tags.forEach(builder::addTag);
        auth.forEach(builder::addAuth);
        return builder;
    }

    @Override
    public Node toNode() {
        if (node == null) {
            node = Node.objectNodeBuilder()
                    .withMember("name", name)
                    .withOptionalMember("tags", !tags.isEmpty()
                            ? Optional.of(ArrayNode.fromStrings(tags))
                            : Optional.empty())
                    .withOptionalMember("auth", !auth.isEmpty()
                            ? Optional.of(ArrayNode.fromStrings(auth))
                            : Optional.empty())
                    .build();
        }

        return node;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Protocol)) {
            return false;
        }

        Protocol protocol = (Protocol) o;
        return name.equals(protocol.name)
               && auth.equals(protocol.auth)
               && tags.equals(protocol.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, auth, tags);
    }

    /**
     * Protocol builder.
     */
    public static final class Builder implements SmithyBuilder<Protocol> {
        private String name;
        private final List<String> auth = new ArrayList<>();
        private final List<String> tags = new ArrayList<>();

        @Override
        public Protocol build() {
            return new Protocol(this);
        }

        /**
         * Sets the protocol name.
         *
         * @param name Protocol name.
         * @return Returns the builder.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Adds an auth scheme to the protocol.
         *
         * @param value Auth scheme to add.
         * @return Returns the builder.
         */
        public Builder addAuth(String value) {
            auth.add(value);
            return this;
        }

        /**
         * Removes an auth scheme from the protocol.
         *
         * @param value Auth scheme to remove.
         * @return Returns the builder.
         */
        public Builder removeAuth(String value) {
            auth.remove(value);
            return this;
        }

        /**
         * Removes all auth schemes from the protocol.
         *
         * @return Returns the builder.
         */
        public Builder clearAuth() {
            auth.clear();
            return this;
        }

        /**
         * Adds a tag to the protocol.
         *
         * @param tag Tag to add.
         * @return Returns the builder.
         */
        public Builder addTag(String tag) {
            tags.add(tag);
            return this;
        }

        /**
         * Removes a tag from the protocol.
         *
         * @param tag Tag to remove.
         * @return Returns the builder.
         */
        public Builder removeTag(String tag) {
            tags.remove(tag);
            return this;
        }

        /**
         * Removes all tags from the protocol.
         *
         * @return Returns the builder.
         */
        public Builder clearTags() {
            tags.clear();
            return this;
        }
    }
}
