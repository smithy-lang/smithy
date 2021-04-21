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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
    private static final String LOCATIONS = "locations";
    private static final Set<String> KEYS = SetUtils.of(
            PREFIX, ALGORITHMS, LOCATIONS);
    private final String prefix;
    private final Set<String> algorithms;
    private final Set<Location> locations;

    private HttpChecksumProperties(HttpChecksumProperties.Builder builder) {
        this.prefix = builder.prefix;
        this.algorithms = builder.algorithms;
        this.locations = builder.locations;
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
        value.getArrayMember(ALGORITHMS).ifPresent(algorithms ->
                Node.loadArrayOfString(ALGORITHMS, algorithms).forEach(builder::addAlgorithm));
        value.getArrayMember(LOCATIONS).ifPresent(locationNodes -> {
            builder.clearLocations();
            locationNodes.forEach(locationNode -> builder.addLocation(Location.fromNode(locationNode)));
        });
        return builder.build();
    }

    /**
     * @return set of locations supported within the HttpChecksum trait.
     */
    public Set<Location> getLocations() {
        return locations;
    }

    /**
     * @return prefix to be used with checksum algorithm to build header or trailer name.
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
                .locations(getLocations());
    }

    /**
     * Converts a value to a {@link Node}.
     *
     * @return Returns the creates Node.
     */
    @Override
    public Node toNode() {
        List<Node> locationNodes = locations.stream().map(Location::toNode).collect(Collectors.toList());
        ObjectNode.Builder builder = Node.objectNodeBuilder()
                .withMember(PREFIX, getPrefix())
                .withMember(ALGORITHMS, Node.fromStrings(algorithms))
                .withMember(LOCATIONS, Node.fromNodes(locationNodes));

        return builder.build();
    }

    public static final class Builder implements SmithyBuilder<HttpChecksumProperties> {
        private String prefix;
        private Set<String> algorithms = new LinkedHashSet<>();
        private Set<Location> locations = new LinkedHashSet<>();

        private Builder() {
            // Add "header" as default supported location.
            locations.add(Location.HEADER);
        }

        @Override
        public HttpChecksumProperties build() {
            return new HttpChecksumProperties(this);
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder locations(Set<Location> locations) {
            clearLocations();
            locations.forEach(this::addLocation);
            return this;
        }

        public Builder addLocation(Location location) {
            this.locations.add(location);
            return this;
        }

        public Builder clearLocations() {
            this.locations.clear();
            return this;
        }

        public Builder algorithms(Set<String> algorithms) {
            clearAlgorithms();
            algorithms.forEach(this::addAlgorithm);
            return this;
        }

        public Builder addAlgorithm(String algorithm) {
            this.algorithms.add(algorithm);
            return this;
        }

        public Builder clearAlgorithms() {
            this.algorithms.clear();
            return this;
        }
    }

    /**
     * Location where the checksum is supplied.
     */
    public enum Location implements ToNode {

        /** The checksum is sent in an HTTP Header. */
        HEADER,

        /** The checksum is sent in a trailer field. */
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
            String value = node.expectStringNode()
                    .expectOneOf("header", "trailer")
                    .toUpperCase(Locale.ENGLISH);
            return Location.valueOf(value);
        }

        /**
         * Returns a Location enum from String.
         *
         * @param location string to map Location enum to.
         * @return Location if location string is valid.
         * @throws IllegalArgumentException when given an invalid location string.
         */
        public static Location fromString(String location) {
            return Location.valueOf(location.toUpperCase(Locale.ENGLISH));
        }
    }
}
