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
import java.util.function.Consumer;
import java.util.function.Supplier;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeIdSyntaxException;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Represents a string node.
 */
public final class StringNode extends Node implements Comparable<StringNode> {
    private String value;

    /**
     * Creates a new StringNode.
     *
     * @param value Immutable value to set.
     * @param sourceLocation Source location of where the node was defined.
     */
    public StringNode(String value, SourceLocation sourceLocation) {
        super(sourceLocation);
        this.value = Objects.requireNonNull(value);
    }

    /**
     * Creates a StringNode that is lazily populated with a value provided by
     * the given {@code Supplier}.
     *
     * <p>This method is used in the {@code SmithyModelLoader} class to be
     * able to resolve unquoted strings to the appropriate shape ID. Because
     * this can only be done after an entire model is loaded, setting the
     * resolved value inside of a node needs to be deferred.
     *
     * <p>Lazy string nodes are not thread safe and there's no validation
     * to ensure that the supplier is invoked only once. Their usage should
     * be rare and you should generally try to use an alternative approach.
     *
     * @param placeholder Placeholder string to use until the supplier is called.
     * @param sourceLocation Location of where the value was originally defined.
     * @return Returns a pair of the string node and the supplier to invoke with the value.
     */
    @SmithyInternalApi
    public static Pair<StringNode, Consumer<String>> createLazyString(
            String placeholder, SourceLocation sourceLocation) {
        StringNode result = new StringNode(placeholder, sourceLocation);
        return Pair.of(result, result::updateValue);
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
    public StringNode expectStringNode(Supplier<String> errorMessage) {
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
     * Updates the value of the string node using the supplier returned
     * from {@link #createLazyString(String, SourceLocation)}.
     *
     * @param value Value to set on the string node.
     */
    private void updateValue(String value) {
        this.value = Objects.requireNonNull(value, "String value cannot be null");
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

    /**
     * Expects that the value of the string is a fully-qualified Shape ID.
     *
     * @return Returns the parsed Shape ID.
     */
    public ShapeId expectShapeId() {
        try {
            return ShapeId.from(getValue());
        } catch (ShapeIdSyntaxException e) {
            throw new SourceException(e.getMessage(), this);
        }
    }

    /**
     * Gets the value of the string as a ShapeId if it is a valid Shape ID.
     *
     * @return Returns the optional Shape ID.
     */
    public Optional<ShapeId> asShapeId() {
        try {
            return Optional.of(ShapeId.from(getValue()));
        } catch (ShapeIdSyntaxException e) {
            return Optional.empty();
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

    @Override
    public int compareTo(StringNode o) {
        return getValue().compareTo(o.getValue());
    }
}
