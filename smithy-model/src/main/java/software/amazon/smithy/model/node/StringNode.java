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

package software.amazon.smithy.model.node;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Represents a string node.
 */
public final class StringNode extends Node {
    private final String value;

    public StringNode(String value, SourceLocation sourceLocation) {
        super(sourceLocation);
        this.value = Objects.requireNonNull(value);
    }

    @Override
    public NodeType getType() {
        return NodeType.STRING;
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.stringNode(this);
    }

    @Override
    public StringNode expectStringNode(String errorMessage) {
        return this;
    }

    @Override
    public Optional<StringNode> asStringNode() {
        return Optional.of(this);
    }

    /**
     * Gets the string value.
     *
     * @return Returns the string value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Gets the string if it is one of the provided valid strings.
     *
     * @param validValues A list of valid string values.
     * @return The string value.
     * @throws ExpectationNotMetException when the value is not one of the
     *  valid strings.
     */
    public String expectOneOf(String... validValues) {
        return expectOneOf(Arrays.asList(validValues));
    }

    /**
     * Gets the string if it is one of the provided valid strings.
     *
     * @param validValues A list of valid string values.
     * @return The string value.
     * @throws ExpectationNotMetException if the value is not in the list.
     */
    public String expectOneOf(Collection<String> validValues) {
        if (!validValues.contains(value)) {
            throw new ExpectationNotMetException(String.format(
                    "Expected one of %s; got `%s`.", ValidationUtils.tickedList(validValues), value), this);
        }
        return value;
    }

    /**
     * Expects that the value of the string is a Shape ID.
     *
     * @param namespace Namespace to use when resolving relative shape IDs.
     * @return Returns the parsed Shape ID.
     */
    public ShapeId expectShapeId(String namespace) {
        try {
            return ShapeId.fromOptionalNamespace(namespace, getValue());
        } catch (ShapeIdSyntaxException e) {
            throw new SourceException(e.getMessage(), this);
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StringNode && value.equals(((StringNode) other).getValue());
    }

    @Override
    public int hashCode() {
        return getType().hashCode() * 7 + value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
