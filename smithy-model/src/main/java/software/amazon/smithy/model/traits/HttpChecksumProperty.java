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
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines checksum property for checksum validation.
 * List of HttpChecksumProperty is used by the members defined for HttpChecksum trait.
 */
public final class HttpChecksumProperty implements ToNode, ToSmithyBuilder<HttpChecksumProperty> {
    private static final String ALGORITHM = "algorithm";
    private static final String LOCATION = "in";
    private static final String NAME = "name";
    private static final Set<String> KEYS = SetUtils.of(ALGORITHM, LOCATION, NAME);

    private final String algorithm;
    private final Location location;
    private final String name;

    private HttpChecksumProperty(HttpChecksumProperty.Builder builder) {
        this.algorithm = Objects.requireNonNull(builder.algorithm);
        this.location = Objects.requireNonNull(builder.location);
        this.name = Objects.requireNonNull(builder.name);
    }

    public static HttpChecksumProperty.Builder builder() {
        return new Builder();
    }

    /**
     * Create a {@code HttpChecksumProperty} from {@link Node}.
     *
     * @param node {@code Node} to create the {@code HttpChecksumProperty}
     * @return Returns the created {@code HttpChecksumProperty}.
     * @throws ExpectationNotMetException if the given {@code node} is invalid.
     */
    public static HttpChecksumProperty fromNode(Node node) {
        ObjectNode value = node.expectObjectNode().warnIfAdditionalProperties(KEYS);
        Builder builder = builder();

        value.getStringMember(ALGORITHM).map(StringNode::getValue).ifPresent(builder::algorithm);
        value.getStringMember(LOCATION).map(StringNode::getValue).ifPresent(builder::location);
        value.getStringMember(NAME).map(StringNode::getValue).ifPresent(builder::name);
        return builder.build();
    }

    /**
     * @return Returns the supported checksum algorithm.
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * @return Returns a location supported for the checksum algorithm.
     */
    public Location getLocation() {
        return location;
    }

    /**
     * @return Returns header or trailer name to be used with checksum algorithm.
     */
    public String getName() {
        return name;
    }

    /**
     * Take this object and create a builder that contains all of the
     * current property values of this object.
     *
     * @return Returns a builder for type HttpChecksumProperties.
     */
    @Override
    public SmithyBuilder<HttpChecksumProperty> toBuilder() {
        return builder()
                .algorithm(getAlgorithm())
                .location(getLocation())
                .name(getName());
    }

    /**
     * Converts a value to a {@link Node}.
     *
     * @return Returns the creates Node.
     */
    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember(ALGORITHM, getAlgorithm())
                .withMember(LOCATION, getLocation().toNode())
                .withMember(NAME, getName())
                .build();
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, location, algorithm);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof HttpChecksumProperty)) {
            return false;
        }

        HttpChecksumProperty that = (HttpChecksumProperty) o;
        return algorithm.equals(that.algorithm)
                && location.equals(that.location)
                && name.equals(that.name);
    }

    /**
     * Determines if the HttpChecksumProperty conflicts with another HttpChecksumProperty.
     *
     * @param otherProperty HttpChecksumProperty to check against.
     * @return Returns true if there is a conflict.
     */
    public boolean conflictsWith(HttpChecksumProperty otherProperty) {
        return algorithm.equals(otherProperty.algorithm)
                && location.equals(otherProperty.location);
    }

    /**
     * Location where the checksum is supplied.
     */
    public enum Location implements ToNode {

        /**
         * The checksum is sent in an HTTP Header.
         */
        HEADER("header"),

        /**
         * The checksum is sent in a trailer field.
         */
        TRAILER("trailer");

        private final String serialized;

        Location(String serialized) {
            this.serialized = serialized;
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
         * @param value string to map Location enum to.
         * @return Location if location string is valid.
         * @throws IllegalArgumentException when given an invalid location string.
         * @throws NullPointerException when given a null location string.
         */
        public static Location fromString(String value) {
            if (value == null) {
                throw new NullPointerException("Found null string argument when converting to Location type");
            }

            value = value.toLowerCase(Locale.ENGLISH);
            for (Location location : values()) {
                if (location.serialized.equals(value)) {
                    return location;
                }
            }
            throw new IllegalArgumentException("Invalid location type: " + value);
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }

        @Override
        public Node toNode() {
            return Node.from(toString());
        }
    }

    public static final class Builder implements SmithyBuilder<HttpChecksumProperty> {
        private String name;
        private String algorithm;
        private Location location;

        @Override
        public HttpChecksumProperty build() {
            return new HttpChecksumProperty(this);
        }

        public Builder algorithm(String algorithm) {
            this.algorithm = algorithm.toLowerCase(Locale.ENGLISH);
            return this;
        }

        public Builder name(String name) {
            this.name = name.toLowerCase(Locale.ENGLISH);
            return this;
        }

        public Builder location(Location location) {
            this.location = location;
            return this;
        }

        public Builder location(String location) {
            this.location = Location.fromString(location);
            return this;
        }
    }
}
