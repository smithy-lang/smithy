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

package software.amazon.smithy.model.traits;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines checksum properties for checksum validation.
 * These properties are used by the members defined for HttpChecksum trait.
 */
public final class HttpChecksumProperties implements ToNode, ToSmithyBuilder<HttpChecksumProperties> {

    private static final String PREFIX = "prefix";
    private static final String ALGORITHMS = "algorithms";
    private static final String LOCATION = "location";
    private static final Set<String> KEYS = SetUtils.of(
            PREFIX, ALGORITHMS, LOCATION);

    private final String prefix;
    private final Set<String> algorithms;
    private final Location location;

    private HttpChecksumProperties(HttpChecksumProperties.Builder builder) {
        this.prefix = builder.prefix;
        this.algorithms = builder.algorithms;
        this.location = builder.location;
    }

    public static HttpChecksumProperties.Builder builder() {
        return new Builder();
    }

    /**
     * Create a {@code HttpChecksumProperties} from {@link Node}.
     *
     * @param node {@code Node} to create the {@code HttpChecksumProperties}
     * @return Returns the created {@code HttpChecksumProperties}.
     * @throws ExpectationNotMetException if the given {@code node} is invalid.
     */
    public static HttpChecksumProperties fromNode(Node node) {
        ObjectNode value = node.expectObjectNode().warnIfAdditionalProperties(KEYS);
        HttpChecksumProperties.Builder builder = builder();
        value.getStringMember(PREFIX).map(StringNode::getValue).ifPresent(builder::prefix);
        value.getMember(ALGORITHMS).ifPresent(
                algorithms -> builder.algorithms(Node.loadArrayOfString(ALGORITHMS, algorithms)
                        .stream().collect(Collectors.toSet())));

        Optional<Node> locationNode = value.getMember(LOCATION);
        if (!locationNode.isPresent()) {
            // set default value for location as HEADER.
            builder.location(Location.HEADER);
        } else {
            builder.location(Location.fromNode(locationNode.get()));
        }

        return builder.build();
    }

    /**
     * @return location member as string. By default, location is `header`.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return prefix member as string.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return set of string denoting the supported checksum algorithms.
     */
    public Set<String> getAlgorithms() {
        return algorithms;
    }

    /**
     * Take this object and create a builder that contains all of the
     * current property values of this object.
     *
     * @return a builder for type HttpChecksumProperties
     */
    @Override
    public SmithyBuilder<HttpChecksumProperties> toBuilder() {
        return builder()
                .prefix(getPrefix())
                .algorithms(getAlgorithms())
                .location(getLocation());
    }

    /**
     * Converts a value to a {@link Node}.
     *
     * @return Returns the creates Node.
     */
    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember(PREFIX, getPrefix())
                .withMember(ALGORITHMS, Node.fromStrings(algorithms))
                .withMember(LOCATION, getLocation().toNode());

        return builder.build();
    }

    public static final class Builder implements SmithyBuilder<HttpChecksumProperties> {
        private String prefix;
        private Location location;
        private Set<String> algorithms = new TreeSet<>();

        private Builder() {
        }

        @Override
        public HttpChecksumProperties build() {
            return new HttpChecksumProperties(this);
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder algorithms(Set<String> algorithms) {
            this.algorithms.clear();
            algorithms.forEach(this::addAlgorithm);
            return this;
        }

        public Builder addAlgorithm(String algorithm) {
            this.algorithms.add(algorithm);
            return this;
        }
    }

    /**
     * Location enum used by HttpChecksumProperties.
     */
    public enum Location implements ToNode {

        /** Location as "header". */
        HEADER,

        /** Location as "trailer". */
        TRAILER;

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public Node toNode() {
            return Node.from(toString());
        }

        /**
         * Create a Location from a Node.
         *
         * @param node Node to create the Location from.
         * @return Returns the created Location.
         * @throws ExpectationNotMetException when given an invalid Node.
         */
        public static Location fromNode(Node node) {
            StringNode value = node.expectStringNode();
            String constValue = value.expectOneOf("header", "trailer").toUpperCase(Locale.ENGLISH);
            return Location.valueOf(constValue);
        }
    }
}
